package com.rs.mymap.data

import android.content.Context
import android.content.SharedPreferences

class ThemePreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = preferences.getBoolean("is_dark_mode", false)
        set(value) = preferences.edit().putBoolean("is_dark_mode", value).apply()
}