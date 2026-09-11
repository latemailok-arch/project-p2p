package com.pingchat.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingchat.android.core.ui.component.sheet.PingchatBottomSheet
import com.pingchat.android.core.ui.component.sheet.PingchatSheetTopBar
import com.pingchat.android.core.ui.component.sheet.PingchatSheetTitle
import com.pingchat.android.geohash.GeohashChannelLevel
import com.pingchat.android.geohash.LocationChannelManager
import com.pingchat.android.R

/**
 * Presenter component for LocationNotesSheet
 * Handles sheet presentation logic with proper error states
 * Extracts this logic from ChatScreen for better separation of concerns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationNotesSheetPresenter(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val locationManager = remember { LocationChannelManager.getInstance(context) }
    val availableChannels by locationManager.availableChannels.collectAsStateWithLifecycle()
    val permissionState by locationManager.permissionState.collectAsStateWithLifecycle()
    val isLoadingLocation by locationManager.isLoadingLocation.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    
    // iOS pattern: notesGeohash ?? LocationChannelManager.shared.availableChannels.first(where: { $0.level == .building })?.geohash
    val buildingGeohash = availableChannels.firstOrNull { it.level == GeohashChannelLevel.BUILDING }?.geohash
    
    if (buildingGeohash != null) {
        // Get location name from locationManager
        val locationNames by locationManager.locationNames.collectAsStateWithLifecycle()
        val locationName = locationNames[GeohashChannelLevel.BUILDING]
            ?: locationNames[GeohashChannelLevel.BLOCK]
        
        LocationNotesSheet(
            geohash = buildingGeohash,
            locationName = locationName,
            nickname = nickname,
            onDismiss = onDismiss
        )
    } else if (permissionState == LocationChannelManager.PermissionState.AUTHORIZED && isLoadingLocation) {
        LocationNotesAcquiringSheet(onDismiss = onDismiss)
    } else {
        // No building geohash available - show error state (matches iOS)
        LocationNotesErrorSheet(
            onDismiss = onDismiss,
            locationManager = locationManager
        )
    }
}

/**
 * Loading sheet when location is being acquired
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationNotesAcquiringSheet(
    onDismiss: () -> Unit
) {
    PingchatBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Acquiring Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Please wait while your location is being determined",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error sheet when location is unavailable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationNotesErrorSheet(
    onDismiss: () -> Unit,
    locationManager: LocationChannelManager
) {
    PingchatBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Location Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Location permission is required for notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    // UNIFIED FIX: Enable location services first (user toggle)
                    locationManager.enableLocationServices()
                    // Then request location channels (which will also request permission if needed)
                    locationManager.enableLocationChannels()
                    locationManager.refreshChannels()
                }) {
                    Text("Enable Location")
                }
            }

            PingchatSheetTopBar(
                onClose = onDismiss,
                modifier = Modifier.align(Alignment.TopCenter),
                title = {
                    PingchatSheetTitle(
                        text = stringResource(R.string.cd_location_notes).uppercase()
                    )
                }
            )
        }
    }
}
