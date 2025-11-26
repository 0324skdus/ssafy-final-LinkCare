package com.a307.linkcare.data

import android.content.Context
import com.a307.linkcare.core.Constants

object ThemePreferences {
    private const val PREFS_NAME = "customize_prefs"

    fun getCharacterId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("characterId", Constants.Defaults.DEFAULT_CHARACTER_ID)
    }

    fun getBackgroundId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("backgroundId", Constants.Defaults.DEFAULT_BACKGROUND_ID)
    }
}
