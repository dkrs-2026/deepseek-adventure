package com.uusse.deepseekadventure.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * API Key 安全存储：EncryptedSharedPreferences + Android Keystore 主密钥。
 * 不写日志、不进 URL、不进崩溃上报。
 */
class SecurePrefs(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun hasKey(): Boolean = getApiKey().isNotBlank()

    /** 仅显示尾部 4 位，禁止明文回显 */
    fun masked(): String {
        val key = getApiKey()
        return if (key.length <= 4) "****" else "****${key.takeLast(4)}"
    }

    companion object {
        private const val PREFS_NAME = "deepseek_secure_prefs"
        private const val KEY_API_KEY = "api_key"
    }
}
