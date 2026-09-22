package com.meteoapp.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stockage persistant de la clé API OpenWeatherMap saisie par l'utilisateur.
 *
 * La clé est chiffrée au repos via une clé AES-256-GCM générée dans
 * l'AndroidKeyStore (matériel sécurisé, non exportable) : remplace
 * `EncryptedSharedPreferences` (androidx.security-crypto), officiellement
 * déprécié, sans dépendance supplémentaire.
 *
 * La valeur est stockée sous forme IV + ciphertext en Base64 dans les
 * SharedPreferences classiques. En cas d'échec du KeyStore (appareil
 * défaillant, restauration de sauvegarde), on retombe sur un stockage en
 * clair pour ne pas bloquer l'application, comme l'implémentation
 * précédente.
 */
object ApiKeyStore {
    private const val PREFS_NAME = "meteo_prefs"
    private const val KEY_API_KEY = "open_weather_api_key"
    private const val KEYSTORE_ALIAS = "meteo_api_key_master"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun masterKey(): SecretKey? = runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        generator.generateKey()
    }.getOrNull()

    private fun encrypt(plain: String): String? {
        val key = masterKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun decrypt(stored: String): String? {
        val key = masterKey() ?: return null
        return runCatching {
            val all = Base64.decode(stored, Base64.NO_WRAP)
            require(all.size > GCM_IV_BYTES)
            val iv = all.copyOfRange(0, GCM_IV_BYTES)
            val encrypted = all.copyOfRange(GCM_IV_BYTES, all.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    /**
     * Heuristique distinguant un ciphertext actuel d'une valeur héritée en
     * clair : un ciphertext est un Base64 assez long (IV 12 o + tag 16 o
     * minimum), une clé API OWM (32 caractères hexadécimaux) ne décode pas
     * en Base64 valide de cette taille.
     */
    private fun looksEncrypted(value: String): Boolean =
        runCatching {
            Base64.decode(value, Base64.NO_WRAP).size > GCM_IV_BYTES + 16
        }.getOrDefault(false)

    fun getApiKey(context: Context): String {
        val stored = prefs(context).getString(KEY_API_KEY, "").orEmpty()
        if (stored.isEmpty()) return ""
        if (looksEncrypted(stored)) {
            return decrypt(stored) ?: ""
        }
        migratePlaintext(context, stored)
        return stored
    }

    private fun migratePlaintext(context: Context, plain: String) {
        val encrypted = encrypt(plain) ?: return
        prefs(context).edit().putString(KEY_API_KEY, encrypted).apply()
    }

    fun setApiKey(context: Context, key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            prefs(context).edit().putString(KEY_API_KEY, "").apply()
            return
        }
        val stored = encrypt(trimmed) ?: trimmed
        prefs(context).edit().putString(KEY_API_KEY, stored).apply()
    }

    fun isConfigured(context: Context): Boolean =
        getApiKey(context).isNotBlank()
}
