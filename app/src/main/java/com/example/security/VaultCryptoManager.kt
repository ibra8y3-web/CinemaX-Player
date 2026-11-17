package com.example.security

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultCryptoManager(private val context: Context) {

    private val vaultDir = File(context.filesDir, "secure_vault").apply {
        if (!exists()) mkdirs()
    }

    private val tempPlayDir = File(context.cacheDir, "vault_playback").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        private const val ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_ALGORITHM = "AES"
        private const val SALT = "NovaVault_Security_Salt_2026_x#"

        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((pin + SALT).toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyPin(enteredPin: String, storedHash: String?): Boolean {
            if (storedHash == null) return false
            return hashPin(enteredPin) == storedHash
        }
    }

    private fun generateKey(pin: String): SecretKeySpec {
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest((pin + SALT).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    suspend fun encryptVideo(
        sourceUri: Uri,
        pin: String,
        outputFileName: String
    ): File = withContext(Dispatchers.IO) {
        val secretKey = generateKey(pin)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encryptedFile = File(vaultDir, outputFileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(encryptedFile).use { fileOut ->
                // Write IV first
                fileOut.write(iv)
                CipherOutputStream(fileOut, cipher).use { cipherOut ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                    }
                    cipherOut.flush()
                }
            }
        } ?: throw IllegalStateException("Cannot open input stream for $sourceUri")

        encryptedFile
    }

    suspend fun decryptToTempForPlayback(
        encryptedFileName: String,
        pin: String
    ): File = withContext(Dispatchers.IO) {
        val encryptedFile = File(vaultDir, encryptedFileName)
        if (!encryptedFile.exists()) {
            throw IllegalArgumentException("الملف المشفر غير موجود")
        }

        val secretKey = generateKey(pin)

        FileInputStream(encryptedFile).use { fileIn ->
            val iv = ByteArray(16)
            val readIv = fileIn.read(iv)
            if (readIv != 16) throw IllegalStateException("خطأ في قراءة مفتاح التشفير IV")
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val tempFile = File(tempPlayDir, "temp_play_${System.currentTimeMillis()}.mp4")

            CipherInputStream(fileIn, cipher).use { cipherIn ->
                FileOutputStream(tempFile).use { fileOut ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                        fileOut.write(buffer, 0, bytesRead)
                    }
                    fileOut.flush()
                }
            }

            tempFile
        }
    }

    fun cleanTempFiles() {
        try {
            tempPlayDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    fun deleteEncryptedFile(fileName: String): Boolean {
        return try {
            File(vaultDir, fileName).delete()
        } catch (_: Exception) {
            false
        }
    }
}
