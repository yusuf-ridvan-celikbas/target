package com.ridvan.target.data

import java.security.SecureRandom

/** Offline password-reset code: 12 unambiguous chars shown as XXXX-XXXX-XXXX; only a salted hash is stored. */
object RecoveryCode {
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // no 0/O/1/I/L
    const val LENGTH = 12

    fun generate(random: SecureRandom = SecureRandom()): String =
        buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    fun format(code: String): String = normalize(code).chunked(4).joinToString("-")

    fun normalize(input: String): String =
        input.uppercase().filter { it != '-' && !it.isWhitespace() }

    fun hash(code: String, salt: String, hasher: (String, String) -> String = PasswordHasher::hash): String =
        hasher(normalize(code), salt)

    fun verify(
        code: String,
        salt: String?,
        expectedHash: String?,
        hasher: (String, String) -> String = PasswordHasher::hash,
    ): Boolean = salt != null && expectedHash != null && hash(code, salt, hasher) == expectedHash
}
