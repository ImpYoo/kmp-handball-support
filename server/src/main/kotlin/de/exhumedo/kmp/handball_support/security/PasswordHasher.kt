package de.exhumedo.kmp.handball_support.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hashes and verifies user passwords.
 */
interface PasswordHasher {
    /**
     * Hashes a plain-text password for persistent storage.
     *
     * @param password Plain-text password.
     * @return Encoded hash containing algorithm metadata and salt.
     */
    fun hash(password: String): String

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param password Plain-text password.
     * @param encodedHash Persisted encoded hash.
     * @return `true` when the password matches.
     */
    fun verify(password: String, encodedHash: String): Boolean
}

/**
 * PBKDF2-HMAC-SHA256 password hasher for server-side credential storage.
 *
 * @property iterations Number of PBKDF2 iterations.
 * @property saltLengthBytes Random salt length.
 * @property keyLengthBits Derived key length.
 */
class Pbkdf2PasswordHasher(
    private val iterations: Int = 210_000,
    private val saltLengthBytes: Int = 16,
    private val keyLengthBits: Int = 256,
) : PasswordHasher {
    private val random = SecureRandom()
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    /**
     * Hashes a password using PBKDF2-HMAC-SHA256.
     */
    override fun hash(password: String): String {
        val salt = ByteArray(saltLengthBytes)
        random.nextBytes(salt)
        val derivedKey = deriveKey(password, salt, iterations, keyLengthBits)

        return listOf(
            "pbkdf2_sha256",
            iterations.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(derivedKey),
        ).joinToString("$")
    }

    /**
     * Verifies a password against a previously encoded hash.
     */
    override fun verify(password: String, encodedHash: String): Boolean {
        val parts = encodedHash.split("$")
        if (parts.size != 4 || parts[0] != "pbkdf2_sha256") {
            return false
        }

        val storedIterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
        val storedKey = runCatching { decoder.decode(parts[3]) }.getOrNull() ?: return false
        val computedKey = deriveKey(password, salt, storedIterations, storedKey.size * 8)

        return MessageDigest.isEqual(storedKey, computedKey)
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int,
    ): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits)
        return SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }
}
