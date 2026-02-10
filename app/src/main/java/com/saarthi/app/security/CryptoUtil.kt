package com.saarthi.app.security

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {

    private const val ITERATIONS = 65536
    private const val KEY_SIZE = 256

    fun encrypt(
        input: File,
        output: File,
        password: String
    ) {

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val key = generateKey(password, salt)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            IvParameterSpec(iv)
        )

        output.outputStream().use { out ->
            out.write(salt)
            out.write(iv)

            input.inputStream().use { inp ->
                val buffer = ByteArray(4096)
                var read: Int
                while (inp.read(buffer).also { read = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, read)
                    if (encrypted != null) out.write(encrypted)
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) out.write(finalBytes)
            }
        }
    }

    private fun generateKey(
        password: String,
        salt: ByteArray
    ): SecretKey {

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_SIZE
        )

        return SecretKeySpec(
            factory.generateSecret(spec).encoded,
            "AES"
        )
    }
}