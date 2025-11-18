package com.a307.linkcare.data

import android.content.Context

object ThemePreferences {
    private const val PREFS_NAME = "customize_prefs"

    fun getCharacterId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("characterId", 1)   // 기본 캐릭터 ID = 1
    }

    fun getBackgroundId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("backgroundId", 1)  // 기본 배경 ID = 1
    }
}
