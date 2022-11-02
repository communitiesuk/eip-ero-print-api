package uk.gov.dluhc.printapi.domain

import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * A globally unique identifier, heavily based on [mongodb's ObjectId class](https://github.com/mongodb/mongo-java-driver/blob/master/bson/src/main/org/bson/types/ObjectId.java)
 *
 * Consists of 12 bytes, divided as follows:
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
 *  `rnd 1` and `rnd 2` are static members, meaning that all instances of [CertificateNumber] generated in the same JVM will have
 *  the same `rnd 1` and `rnd 2` values. The net result is that bytes [4, 5, 6, 7, 8] of 2 instances generated in the same JVM
 *  will be the same.
 *
 *  `increment` is an [AtomicInteger] and is a static member seeded from a random number. The use of `getAndIncrement()` means
 *  this is a thread safe way of generating an incremental counter.
 *
 * An instance of [CertificateNumber] is a 12 byte identifier which serializes as a 20 character string using the character
 * set `0123456789ACDEFGHJKLMNPQRTUVWXYZ` (specifically B, I, O and S are excluded as they can be misread as numbers)
 *
 * Class constructors are convenience constructors for the purpose of tests. The public API and preferred method of
 * creating a new Certificate Number is to use the static factory method [CertificateNumber.create]
 */
class CertificateNumber {

    private val timestamp: Int
    private val randomValue1: Int
    private val randomValue2: Short
    private val counter: Int

    companion object {
        private const val LOW_ORDER_THREE_BYTES = 0x00ffffff

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

        private fun dateToTimestampSeconds(date: Instant): Int =
            date.epochSecond.toInt()

        /**
         * Creates a new 20 character string Certificate Number.
         */
        fun create(): String =
            CertificateNumber().toString()
    }

    constructor() : this(
        timestamp = dateToTimestampSeconds(Instant.now()),
        randomValue1 = RANDOM_VALUE1,
        randomValue2 = RANDOM_VALUE2,
        counter = NEXT_COUNTER.getAndIncrement(),
    )

    constructor(value: String) {
        require(value.length == 20) { "CertificateNumber value length must be 20" }
        require(value.toCharArray().all { it in ALPHABET }) { "CertificateNumber value must only contain characters from [${ALPHABET.joinToString("")}]" }
        require(with(value[19]) { this == '0' || this == '1' }) { "CertificateNumber value last character must be a 0 or 1" }

        val ninetySixBits = value.toCharArray().mapIndexed { idx, char ->
            // map each character in the string to the index of the char in the alphabet - 5 bits for all but the last which is 1 bit = 96 bits
            ALPHABET.indexOf(char).asBinaryString(if (idx < 19) 5 else 1)
        }.joinToString("")

        val timestamp = Integer.parseInt(ninetySixBits.substring(0, 32), 2) // first 32 bits (4 bytes) is the timestamp
        val randomValue1 = Integer.parseInt(ninetySixBits.substring(32, 56), 2) // next 24 bits (3 bytes) is random 1
        val randomValue2 = Integer.parseInt(ninetySixBits.substring(56, 72), 2).toShort() // next 16 bits (2 bytes) is random 2
        val counter = Integer.parseInt(ninetySixBits.substring(72, 96), 2) // last 24 bits (3 bytes) is the counter

        validateValues(randomValue1, counter)
        this.timestamp = timestamp
        this.randomValue1 = randomValue1
        this.randomValue2 = randomValue2
        this.counter = counter
    }

    constructor(timestamp: Int, randomValue1: Int, randomValue2: Short, counter: Int) {
        validateValues(randomValue1, counter)
        this.timestamp = timestamp
        this.randomValue1 = randomValue1
        this.randomValue2 = randomValue2
        this.counter = counter and LOW_ORDER_THREE_BYTES
    }

    override fun toString(): String {
        val ninetySixBits = "${timestamp.asBinaryString(32)}${randomValue1.asBinaryString(24)}${randomValue2.asBinaryString(16)}${counter.asBinaryString(24)}"
        return ninetySixBits.chunked(5) {
            val fiveBits = Integer.parseInt(it.toString(), 2)
            ALPHABET[fiveBits]
        }.joinToString("")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CertificateNumber

        if (timestamp != other.timestamp) return false
        if (counter != other.counter) return false
        if (randomValue1 != other.randomValue1) return false
        if (randomValue2 != other.randomValue2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp
        result = 31 * result + counter
        result = 31 * result + randomValue1
        result = 31 * result + randomValue2
        return result
    }

    private fun validateValues(randomValue1: Int, counter: Int) {
        require(randomValue1 and -0x1000000 == 0) { "The random value must be between 0 and 16777215 (it must fit in three bytes)." }
        require(counter and -0x1000000 == 0) { "The counter must be between 0 and 16777215 (it must fit in three bytes)." }
    }

    private fun Int.asBinaryString(numberOfBits: Int): String =
        this.toString(2).padStart(numberOfBits, '0')

    private fun Short.asBinaryString(numberOfBits: Int): String =
        this.toString(2).padStart(numberOfBits, '0')
}
