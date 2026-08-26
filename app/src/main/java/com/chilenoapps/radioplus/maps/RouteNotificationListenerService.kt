package com.chilenoapps.radioplus.maps

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class RouteNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != GOOGLE_MAPS_PACKAGE) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        ).mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }.distinct().joinToString(" • ")
        if (title.isBlank() && text.isBlank()) return
        sendBroadcast(Intent(ACTION_ROUTE_UPDATE).setPackage(packageName).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_TEXT, text))
    }

    companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
        const val ACTION_ROUTE_UPDATE = "com.chilenoapps.radioplus.ROUTE_UPDATE"
        const val EXTRA_TITLE = "route_title"
        const val EXTRA_TEXT = "route_text"
    }
}
