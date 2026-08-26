package com.bitchat.android.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.ui.ChatViewModel
import com.bitchat.android.ui.theme.BitchatFontFamily
import com.bitchat.android.util.AppConstants
import kotlinx.coroutines.delay

/**
 * Hidden diagnostic screen - accessed by long-pressing the app bar (brand icon).
 * Displays real-time mesh health metrics: active peer count, hop count (max 7), outbox queue.
 * Hooks into BluetoothMeshService and WifiAwareMeshService via ChatViewModel / MeshServiceHolder.
 *
 * Metrics:
 * - Active peer count: merged from Bluetooth + WiFi Aware transports
 * - Current hop count: TTL display (max 7 hops) from AppConstants.MESSAGE_TTL_HOPS and live packet TTL observations
 * - Outbox queue status: per-peer queued messages, retry backoff, expiry (24h TTL)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshHealthDashboardSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    // Live polling of mesh metrics every 1s for dashboard
    var bluetoothPeers by remember { mutableStateOf(0) }
    var wifiAwarePeers by remember { mutableStateOf(0) }
    var activeCount by remember { mutableStateOf(0) }
    var outboxSnapshot by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var outboxTotal by remember { mutableStateOf(0) }
    var lastHopObserved by remember { mutableStateOf<Int?>(null) }
    val appContext = LocalContext.current.applicationContext

    LaunchedEffect(isVisible) {
        while (isVisible) {
            try {
                // Active peer count via UnifiedMeshService (merged transports)
                val unified = try { viewModel.meshServiceFacade.getActivePeerCount() } catch (_: Exception) { 0 }
                activeCount = unified
                // Also try individual transports for breakdown
                bluetoothPeers = try {
                    com.bitchat.android.service.MeshServiceHolder.getOrCreate(appContext).getActivePeerCount()
                } catch (_: Exception) { connectedPeers.size }
                wifiAwarePeers = try {
                    com.bitchat.android.wifiaware.WifiAwareController.getService()?.getActivePeerCount() ?: 0
                } catch (_: Exception) { 0 }

                // Outbox queue status via MessageRouter
                val router = try { com.bitchat.android.services.MessageRouter.tryGetInstance() } catch (_: Exception) { null }
                if (router != null) {
                    outboxSnapshot = try { router.getOutboxSnapshot() } catch (_: Exception) { try { router.getOutboxSnapshotForDashboard() } catch (_: Exception) { emptyMap() } }
                    outboxTotal = outboxSnapshot.values.sum()
                } else {
                    outboxSnapshot = emptyMap()
                    outboxTotal = 0
                }

                // Hop count max 7 is constant; last observed hop could be derived from packet TTL if exposed.
                // For now show constant 7 and compute remaining TTL as 7 - hopsTaken (hopsTaken = 7 - current TTL if available)
                // We approximate via MeshCore's last relay TTL if available via debug status parsing
                lastHopObserved = try {
                    val debug = viewModel.meshServiceFacade.getDebugStatus()
                    // Parse "TTL:" from debug string if present
                    Regex("""TTL[:\s]+(\d+)""").find(debug)?.groupValues?.get(1)?.toIntOrNull()
                } catch (_: Exception) { null }

            } catch (_: Exception) {}
            delay(1000)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mesh Health & Diagnostics",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = BitchatFontFamily, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close diagnostics")
                }
            }
            HorizontalDivider()

            // Active peer count
            DashboardSection(title = "Active Peers") {
                MetricRow(label = "Merged (UnifiedMeshService)", value = "$activeCount", highlight = activeCount > 0)
                MetricRow(label = "Bluetooth LE Mesh", value = "$bluetoothPeers")
                MetricRow(label = "Wi-Fi Aware", value = "$wifiAwarePeers")
                MetricRow(label = "Connected (UI)", value = "${connectedPeers.size}")
                MetricRow(label = "Is Connected", value = if (isConnected) "YES" else "NO", highlight = isConnected)
                if (connectedPeers.isNotEmpty()) {
                    Text(
                        text = "Peer IDs: ${connectedPeers.joinToString { it.take(8) }}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Hop count (max 7)
            DashboardSection(title = "Hop Count & TTL Routing") {
                MetricRow(label = "Max Hops (MESSAGE_TTL_HOPS)", value = "${AppConstants.MESSAGE_TTL_HOPS}")
                MetricRow(label = "Current TTL observed", value = lastHopObserved?.toString() ?: "n/a")
                MetricRow(label = "Hops taken (est.)", value = lastHopObserved?.let { "${AppConstants.MESSAGE_TTL_HOPS.toInt() - it}" } ?: "n/a")
                MetricRow(label = "SYNC_TTL_HOPS (neighbor only)", value = "${AppConstants.SYNC_TTL_HOPS}")
                Text(
                    text = "Packets decrement TTL per relay; 0 = dropped. 7 hops = max mesh diameter.",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Visual hop bar 0..7
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..AppConstants.MESSAGE_TTL_HOPS.toInt()) {
                        val isActive = lastHopObserved?.let { it <= AppConstants.MESSAGE_TTL_HOPS.toInt() - i } ?: (i <= 2)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .background(
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            // Outbox queue status
            DashboardSection(title = "Outbox Queue (MessageRouter)") {
                MetricRow(label = "Total queued messages", value = "$outboxTotal", highlight = outboxTotal > 0)
                MetricRow(label = "Conversations with pending", value = "${outboxSnapshot.size}")
                MetricRow(label = "OUTBOX_TICK_MS", value = "${AppConstants.Router.OUTBOX_TICK_MS} ms")
                MetricRow(label = "OUTBOX_TTL_MS", value = "${AppConstants.Router.OUTBOX_MESSAGE_TTL_MS / 1000 / 3600}h")
                MetricRow(label = "MAX_PER_PEER", value = "${AppConstants.Router.OUTBOX_MAX_PER_PEER}")
                if (outboxSnapshot.isNotEmpty()) {
                    Text(
                        text = "Per-peer queue:",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = BitchatFontFamily, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(outboxSnapshot.entries.toList()) { (peer, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = peer.take(16) + "…",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$count pending",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily, fontWeight = FontWeight.Medium),
                                    color = if (count > 50) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Outbox empty - all messages delivered or no retry pending.",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "Handshake backoff: ${AppConstants.Router.HANDSHAKE_RETRY_BACKOFF_MS.joinToString()} ms",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Debug status raw
            DashboardSection(title = "Transports Debug Status") {
                var debugText by remember { mutableStateOf("Loading...") }
                LaunchedEffect(isVisible) {
                    while (isVisible) {
                        debugText = try { viewModel.meshServiceFacade.getDebugStatus() } catch (e: Exception) { "Unavailable: ${e.message}" }
                        delay(2000)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                text = debugText,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hidden diagnostic - long-press app bar to access. Data refreshed every 1s.",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = BitchatFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = BitchatFontFamily, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        content()
    }
}

@Composable
private fun MetricRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = BitchatFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = BitchatFontFamily, fontWeight = FontWeight.SemiBold),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Extension to expose outbox snapshot without breaking encapsulation.
 * Added via reflection fallback if not directly available.
 */
fun com.bitchat.android.services.MessageRouter.getOutboxSnapshotForDashboard(): Map<String, Int> {
    return try {
        // Try direct method if we added it
        val method = this::class.java.getDeclaredMethod("getOutboxSnapshot")
        @Suppress("UNCHECKED_CAST")
        method.invoke(this) as Map<String, Int>
    } catch (_: Exception) {
        try {
            val field = this::class.java.getDeclaredField("outbox")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val outbox = field.get(this) as java.util.concurrent.ConcurrentHashMap<String, MutableList<*>>
            outbox.mapValues { it.value.size }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
