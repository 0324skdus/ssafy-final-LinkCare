package com.linkcare.linkcarewear.data

import android.content.Context

object ThemePreferences {
    private const val PREFS_NAME = "character_prefs"

    fun getCharacterId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("selected_character_id", 1001L) // 기본 오리
    }

    fun getBackgroundId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("selected_background_id", 2001L) // 기본 배경
    }
}
