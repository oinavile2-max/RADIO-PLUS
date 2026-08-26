package com.chilenoapps.radioplus.settings

import android.content.Context

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("radio_plus_settings", Context.MODE_PRIVATE)

    var sidePanelDelaySeconds: Int
        get() = preferences.getInt("side_panel_delay", 5)
        set(value) = preferences.edit().putInt("side_panel_delay", value).apply()

    var sidePanelPinned: Boolean
        get() = preferences.getBoolean("side_panel_pinned", false)
        set(value) = preferences.edit().putBoolean("side_panel_pinned", value).apply()

    var startOnPhysicalRadio: Boolean
        get() = preferences.getBoolean("start_physical_radio", true)
        set(value) = preferences.edit().putBoolean("start_physical_radio", value).apply()

    var nightMode: Boolean
        get() = preferences.getBoolean("night_mode", false)
        set(value) = preferences.edit().putBoolean("night_mode", value).apply()

    var routePopups: Boolean
        get() = preferences.getBoolean("route_popups", true)
        set(value) = preferences.edit().putBoolean("route_popups", value).apply()

    var musicPopups: Boolean
        get() = preferences.getBoolean("music_popups", true)
        set(value) = preferences.edit().putBoolean("music_popups", value).apply()

    var videoMotionLock: Boolean
        get() = preferences.getBoolean("video_motion_lock", true)
        set(value) = preferences.edit().putBoolean("video_motion_lock", value).apply()

    var autoResumeBluetooth: Boolean
        get() = preferences.getBoolean("auto_resume_bluetooth", true)
        set(value) = preferences.edit().putBoolean("auto_resume_bluetooth", value).apply()

    var voicePortuguese: Boolean
        get() = preferences.getBoolean("voice_pt_br", true)
        set(value) = preferences.edit().putBoolean("voice_pt_br", value).apply()
}
