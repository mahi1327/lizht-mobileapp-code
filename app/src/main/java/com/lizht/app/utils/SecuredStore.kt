package com.lizht.app.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecuredStore(context: Context) {

    private val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "auth_store",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(access: String, refresh: String) {
        prefs.edit().putString("access", access).putString("refresh", refresh).apply()
    }

    fun getAccess(): String?  = prefs.getString("access", null)
    fun getRefresh(): String? = prefs.getString("refresh", null)

    fun saveUserJson(json: String) {
        prefs.edit().putString("user_json", json).apply()
    }
    fun getUserJson(): String? = prefs.getString("user_json", null)

    fun hasSession(): Boolean = !getAccess().isNullOrEmpty() && !getRefresh().isNullOrEmpty()

    fun clear() { prefs.edit().clear().apply() }
}
