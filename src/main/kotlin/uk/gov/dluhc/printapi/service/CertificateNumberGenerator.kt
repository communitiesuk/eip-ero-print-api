package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * A generator of globally unique Certificate Numbers, heavily based on [mongodb's ObjectId class](https://github.com/mongodb/mongo-java-driver/blob/master/bson/src/main/org/bson/types/ObjectId.java)
 *
 * A Certificate Number is a 20 character string using the character set `0123456789ACDEFGHJKLMNPQRTUVWXYZ`
 * (specifically B, I, O and S are excluded as they can be misread as numbers)
 *
 * The Certificate Number is derived from 12 bytes, arranged as follows:
 *
 * ```
 *   | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
 *   |---------------|-----------|-------|-------------|
 *   | time          |rnd 1      | rnd 2 | increment   |
 * ```
 *
 * Where:
 *   * `time` is the timestamp since epoch rounded to second precision. The first 4 bytes of the integer are used.
 *   * `rnd 1` is a random integer. This is a static member. The first 3 bytes of the integer are used.
 *   * `rnd 2` is a random short. This is a static member. The first 2 bytes of the short are used.
 *   * `increment` is a sequential increment, starting from a random integer. The first 3 bytes of the integer are used.
 *
 *  `rnd 1` and `rnd 2` are static members, meaning that all Certificate Numbers generated in the same JVM will have
 *  the same `rnd 1` and `rnd 2` values. The net result is that bytes [4, 5, 6, 7, 8] of 2 instances generated in the same JVM
 *  will be the same.
 *
 *  `increment` is an [AtomicInteger] and is a static member seeded from a random number. The use of `getAndIncrement()` means
 *  this is a thread safe way of generating an incremental counter.
 *
 * The generated Certificate Number is a serialization of the 12 bytes.
 */
@Component
class CertificateNumberGenerator(private val clock: Clock) {

    companion object {
        // Use primitives to represent the 5-byte random value.
        private val RANDOM_VALUE1: Int = SecureRandom().nextInt(0x01000000)
        private val RANDOM_VALUE2: Short = SecureRandom().nextInt(0x00008000).toShort()

        private val NEXT_COUNTER = AtomicInteger(SecureRandom().nextInt(0x01000000))

        private val ALPHABET = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L',
            'M', 'N', 'P', 'Q', 'R', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z'
        )
    }

    fun generateCertificateNumber(): String {
        val timestamp = dateToTimestampSeconds(Instant.now(clock))
        val randomValue1 = RANDOM_VALUE1
        val randomValue2 = RANDOM_VALUE2
        val counter = NEXT_COUNTER.getAndIncrement()

        val ninetySixBits = "${timestamp.asBinaryString(32)}${randomValue1.asBinaryString(24)}${randomValue2.asBinaryString(16)}${counter.asBinaryString(24)}"
        return ninetySixBits.chunked(5) {
            val fiveBits = Integer.parseInt(it.toString(), 2)
            ALPHABET[fiveBits]
        }.joinToString("")
    }

    private fun dateToTimestampSeconds(date: Instant): Int =
        date.epochSecond.toInt()

    private fun Int.asBinaryString(numberOfBits: Int): String =
        this.toString(2).padStart(numberOfBits, '0')

    private fun Short.asBinaryString(numberOfBits: Int): String =
        this.toString(2).padStart(numberOfBits, '0')
}
