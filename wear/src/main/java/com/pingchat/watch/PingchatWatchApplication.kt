package com.pingchat.watch

import android.app.Application
import com.pingchat.android.mesh.PowerManager
import com.pingchat.watch.notification.WearNotificationCoordinator
import com.pingchat.watch.ui.WearPeerIdentityState

class PingchatWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PowerManager.getInstance(applicationContext)
        WearNotificationCoordinator.getInstance(applicationContext)
        WearPeerIdentityState.initialize(applicationContext)
    }
}
