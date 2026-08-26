package com.bitchat.android.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.geohash.Geohash
import com.bitchat.android.geohash.LocationChannelManager
import com.bitchat.android.ui.ChatViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Local Mesh Map - OpenStreetMap tile layer rendering nearby active nodes.
 * Reads Geohash data from local Nostr location channels (LocationChannelManager)
 * and renders them as markers. Also overlays mesh peer count as additional markers
 * offset around geohash center to simulate "nearby active nodes" distribution.
 *
 * Uses org.osmdroid:osmdroid-android - Offline caching via osmdroid's built-in tile cache
 * (tiles persist in osmdroid/tiles cache dir, survive offline).
 *
 * Grill edge cases:
 * - No geohash channels available -> center on default (SF) and show empty state
 * - Offline tile load -> osmdroid serves cached tiles, shows grey if not cached (graceful)
 * - Null location -> fallback to first geohash center
 * - Permission denied -> still shows map at geohash center, no my-location overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMeshMapScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val availableChannels by remember { LocationChannelManager.getInstance(context).availableChannels }.collectAsStateWithLifecycle()
    val geohashPeople by viewModel.geohashPeople.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedLocationChannel.collectAsStateWithLifecycle()

    // Determine center: selected geohash -> first available -> default (Berlin/SF fallback)
    val mapCenter: GeoPoint = remember(availableChannels, selectedChannel) {
        val targetGeohash: String? = when (val ch = selectedChannel) {
            is com.bitchat.android.geohash.ChannelID.Location -> ch.channel.geohash
            else -> availableChannels.firstOrNull()?.geohash
        }
        if (targetGeohash != null) {
            val (lat, lon) = Geohash.decodeToCenter(targetGeohash)
            GeoPoint(lat, lon)
        } else {
            // Fallback: San Francisco (common demo) - 37.7749, -122.4194
            GeoPoint(37.7749, -122.4194)
        }
    }

    // Initialize osmdroid config once
    LaunchedEffect(Unit) {
        try {
            Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context))
        } catch (_: Exception) {}
        Configuration.getInstance().userAgentValue = context.packageName
        // Enable aggressive tile caching: osmdroid default is 512MB, we keep that
        try {
            Configuration.getInstance().osmdroidBasePath = context.cacheDir
            Configuration.getInstance().osmdroidTileCache = context.cacheDir.resolve("osmdroid_tiles").apply { mkdirs() }
        } catch (_: Exception) {}
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Lifecycle handling for MapView
    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onPause()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Mesh Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        try { Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx)) } catch (_: Exception) {}
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        controller.setCenter(mapCenter)
                        // Cache settings
                        minZoomLevel = 3.0
                        maxZoomLevel = 19.0
                        isTilesScaledToDpi = true

                        // My location overlay (gracefully handles permission denial)
                        try {
                            val provider = GpsMyLocationProvider(ctx)
                            myLocationOverlay = MyLocationNewOverlay(provider, this).apply {
                                enableMyLocation()
                                enableFollowLocation()
                                isDrawAccuracyEnabled = true
                            }
                            overlays.add(myLocationOverlay)
                        } catch (_: Exception) {}

                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    // Clear previous geohash markers but keep my location
                    val toRemove = mapView.overlays.filterIsInstance<Marker>().toList()
                    toRemove.forEach { mapView.overlays.remove(it) }

                    // Render Geohash channels as primary markers (from local Nostr location channels)
                    availableChannels.forEach { channel ->
                        val (lat, lon) = Geohash.decodeToCenter(channel.geohash)
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(lat, lon)
                            title = "${channel.level.displayName} • ${channel.geohash}"
                            snippet = "Nostr location channel • ${channel.geohash.length} chars • tap to join"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            // Use distinct icon per level
                            icon = ctxCompatDrawable(ctx = context, level = channel.level)
                        }
                        mapView.overlays.add(marker)

                        // Bounding box rectangle for geohash cell
                        try {
                            val bounds = Geohash.decodeToBounds(channel.geohash)
                            // Add simple polygon overlay for cell bounds (optional visual)
                            // Using Marker snippet to show bounds size; full polygon omitted for simplicity to keep offline performant
                        } catch (_: Exception) {}
                    }

                    // Render nearby active nodes: geohash participants + mesh peers
                    // Geohash participants have Nostr pubkeys but not lat/lon; we place them jittered around channel center
                    val primaryCenter = mapCenter
                    geohashPeople.forEachIndexed { idx, person ->
                        // Jitter around primary center to visualize distinct nodes (deterministic offset from pubkey hash)
                        val hash = person.id.hashCode()
                        val latOffset = ((hash % 100) / 10000.0) + (idx * 0.0003)
                        val lonOffset = (((hash / 100) % 100) / 10000.0) + (idx * 0.0003)
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(primaryCenter.latitude + latOffset, primaryCenter.longitude + lonOffset)
                            title = person.displayName.ifBlank { "Anonymous" }
                            snippet = "Geohash participant • ${person.id.take(8)}..."
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            // Small dot style
                            icon = ctxCompatDrawableSmall(context)
                        }
                        mapView.overlays.add(marker)
                    }

                    // Mesh peers without geolocation - show clustered around mapCenter as well, distinct color
                    connectedPeers.take(20).forEachIndexed { idx, peerID ->
                        // Deterministic offset from peerID hash
                        val hash = peerID.hashCode()
                        val latOffset = ((hash % 80) / 8000.0) - 0.005
                        val lonOffset = (((hash / 80) % 80) / 8000.0) - 0.005
                        // Skip overlapping with geohash people
                        if (idx < geohashPeople.size) return@forEachIndexed
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(primaryCenter.latitude + latOffset, primaryCenter.longitude + lonOffset)
                            title = peerID.take(12)
                            snippet = "Mesh peer • ${if (connectedPeers.contains(peerID)) "connected" else "known"} • ${peerID.take(8)}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = ctxCompatDrawableMesh(context)
                        }
                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Bottom info card: active nodes count + geohash channels
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Nearby Active Nodes: ${connectedPeers.size + geohashPeople.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Channels: ${availableChannels.joinToString { it.geohash } .ifBlank { "none - enable location" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Geohash participants: ${geohashPeople.size} • Mesh peers: ${connectedPeers.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tile source: OpenStreetMap (offline cache enabled)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // My location FAB
            FloatingActionButton(
                onClick = {
                    try {
                        mapViewRef?.controller?.animateTo(mapCenter)
                        mapViewRef?.controller?.setZoom(15.0)
                        myLocationOverlay?.enableFollowLocation()
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 96.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Center on location")
            }
        }
    }
}

private fun ctxCompatDrawable(ctx: Context, level: com.bitchat.android.geohash.GeohashChannelLevel): android.graphics.drawable.Drawable? {
    return try {
        val resId = when (level) {
            com.bitchat.android.geohash.GeohashChannelLevel.BUILDING -> com.bitchat.android.R.drawable.ic_spec_people
            com.bitchat.android.geohash.GeohashChannelLevel.NEIGHBORHOOD -> com.bitchat.android.R.drawable.ic_spec_globe
            else -> com.bitchat.android.R.drawable.ic_spec_range
        }
        androidx.core.content.ContextCompat.getDrawable(ctx, resId)
    } catch (_: Exception) { null }
}

private fun ctxCompatDrawableSmall(ctx: Context): android.graphics.drawable.Drawable? {
    return try { androidx.core.content.ContextCompat.getDrawable(ctx, com.bitchat.android.R.drawable.ic_spec_people) } catch (_: Exception) { null }
}

private fun ctxCompatDrawableMesh(ctx: Context): android.graphics.drawable.Drawable? {
    return try { androidx.core.content.ContextCompat.getDrawable(ctx, com.bitchat.android.R.drawable.ic_spec_bluetooth) } catch (_: Exception) { null }
}
