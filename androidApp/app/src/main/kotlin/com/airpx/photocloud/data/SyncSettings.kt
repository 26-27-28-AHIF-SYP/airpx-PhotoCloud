package com.airpx.photocloud.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import java.util.Base64

private val Context.settingsStore by preferencesDataStore(name = "photocloud_settings")

class SyncSettings(private val context: Context) {
    private val serverKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("access_token")
    private val wifiKey = stringPreferencesKey("wifi_only")
    private val automaticKey = stringPreferencesKey("automatic_backup")

    suspend fun saveServer(url: String) = context.settingsStore.edit { it[serverKey] = url.trimEnd('/') }
    suspend fun saveToken(token: String) = context.settingsStore.edit { it[tokenKey] = Base64.getEncoder().encodeToString(aead().encrypt(token.toByteArray(), null)) }
    suspend fun saveWifiOnly(enabled: Boolean) = context.settingsStore.edit { it[wifiKey] = enabled.toString() }
    suspend fun saveAutomatic(enabled: Boolean) = context.settingsStore.edit { it[automaticKey] = enabled.toString() }
    suspend fun server() = context.settingsStore.data.first()[serverKey].orEmpty()
    suspend fun token(): String? = context.settingsStore.data.first()[tokenKey]?.let {
        String(aead().decrypt(Base64.getDecoder().decode(it), null))
    }
    suspend fun wifiOnly() = context.settingsStore.data.first()[wifiKey]?.toBoolean() ?: false
    suspend fun automatic() = context.settingsStore.data.first()[automaticKey]?.toBoolean() ?: false

    private fun aead(): Aead {
        AeadConfig.register()
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, "photo_cloud_keyset", "photo_cloud_keyset")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://airpx-photocloud")
            .build().keysetHandle.getPrimitive(Aead::class.java)
    }
}
