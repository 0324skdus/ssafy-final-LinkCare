package com.a307.linkcare.common.network.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.a307.linkcare.feature.auth.data.model.response.LoginResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("auth_prefs")

object TokenKeys {
    val ACCESS = stringPreferencesKey("access")
    val REFRESH = stringPreferencesKey("refresh")
    val EMAIL = stringPreferencesKey("email")
    val NAME = stringPreferencesKey("name")
    val USER_PK = longPreferencesKey("userPk")
}

class TokenStore(private val context: Context) {
    fun getAccess(): String? = runBlocking {
        context.dataStore.data.first()[TokenKeys.ACCESS]
    }
    fun getRefresh(): String? = runBlocking {
        context.dataStore.data.first()[TokenKeys.REFRESH]
    }
    fun getUserPk(): Long? = runBlocking {
        context.dataStore.data.first()[TokenKeys.USER_PK]
    }
    fun getName(): String? = runBlocking {
        context.dataStore.data.first()[TokenKeys.NAME]
    }
    fun save(auth: LoginResponse) = runBlocking {
        context.dataStore.edit {
            it[TokenKeys.ACCESS] = auth.accessToken
            it[TokenKeys.REFRESH] = auth.refreshToken
            it[TokenKeys.EMAIL] = auth.email
            it[TokenKeys.NAME] = auth.name
            it[TokenKeys.USER_PK] = auth.userPk
        }
    }
    fun saveTokens(access: String, refresh: String) = runBlocking {
        context.dataStore.edit {
            it[TokenKeys.ACCESS] = access
            it[TokenKeys.REFRESH] = refresh
        }
    }
    fun clear() = runBlocking {
        context.dataStore.edit {
            it.remove(TokenKeys.ACCESS)
            it.remove(TokenKeys.REFRESH)
            it.remove(TokenKeys.EMAIL)
            it.remove(TokenKeys.NAME)
            it.remove(TokenKeys.USER_PK)
        }
    }
}
