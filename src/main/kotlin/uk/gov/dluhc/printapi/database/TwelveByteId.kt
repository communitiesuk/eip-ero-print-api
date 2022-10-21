package uk.gov.dluhc.printapi.database

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Date
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
 *  `rnd 1` and `rnd 2` are static members, meaning that all instances of [TwelveByteId] generated in the same JVM will have
 *  the same `rnd 1` and `rnd 2` values. The net result is that bytes [4, 5, 6, 7, 8] of 2 instances generated in the same JVM
 *  will be the same.
 *
 *  `increment` is an [AtomicInteger] and is a static member seeded from a random number. The use of `getAndIncrement()` means
 *  this is a thread safe way of generating an incremental counter.
 *
 * An instance of [TwelveByteId] is a 12 byte identifier which serializes via it's `toString()` method into a 24 character hex string.
 */
class TwelveByteId {

    private val timestamp: Int
    private val counter: Int
    private val randomValue1: Int
    private val randomValue2: Short

    companion object {
        private const val OBJECT_ID_LENGTH = 12
        private const val LOW_ORDER_THREE_BYTES = 0x00ffffff

        // Use primitives to represent the 5-byte random value.
        private val RANDOM_VALUE1: Int = SecureRandom().nextInt(0x01000000)
        private val RANDOM_VALUE2: Short = SecureRandom().nextInt(0x00008000).toShort()

        private val NEXT_COUNTER = AtomicInteger(SecureRandom().nextInt())

        private val HEX_CHARS = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        )

        private fun dateToTimestampSeconds(date: Date): Int =
            (date.time / 1000).toInt()

        fun get(): TwelveByteId = TwelveByteId()
    }

    constructor() : this(Date())

    private constructor(date: Date) : this(
        dateToTimestampSeconds(date),
        NEXT_COUNTER.getAndIncrement() and LOW_ORDER_THREE_BYTES,
        false
    )

    private constructor(timestamp: Int, counter: Int, checkCounter: Boolean) : this(
        timestamp,
        RANDOM_VALUE1,
        RANDOM_VALUE2,
        counter,
        checkCounter
    )

    private constructor(timestamp: Int, randomValue1: Int, randomValue2: Short, counter: Int, checkCounter: Boolean) {
        require(randomValue1 and -0x1000000 == 0) { "The random value must be between 0 and 16777215 (it must fit in three bytes)." }
        require(!(checkCounter && counter and -0x1000000 != 0)) { "The counter must be between 0 and 16777215 (it must fit in three bytes)." }
        this.timestamp = timestamp
        this.counter = counter and LOW_ORDER_THREE_BYTES
        this.randomValue1 = randomValue1
        this.randomValue2 = randomValue2
    }

    override fun toString(): String = toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TwelveByteId

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

    private fun toHexString(): String {
        val chars = CharArray(OBJECT_ID_LENGTH * 2)
        var i = 0
        for (b in toByteArray()) {
            chars[i++] = HEX_CHARS[b.toInt() shr 4 and 0xF]
            chars[i++] = HEX_CHARS[b.toInt() and 0xF]
        }
        return String(chars)
    }

    private fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(OBJECT_ID_LENGTH)
        putToByteBuffer(buffer)
        return buffer.array()
    }

    private fun putToByteBuffer(buffer: ByteBuffer) {
        requireNotNull(buffer) { "buffer cannot be null" }
        require(buffer.remaining() >= OBJECT_ID_LENGTH) { "buffer.remaining() >=12" }

        buffer
            .put(int3(timestamp))
            .put(int2(timestamp))
            .put(int1(timestamp))
            .put(int0(timestamp))
            .put(int2(randomValue1))
            .put(int1(randomValue1))
            .put(int0(randomValue1))
            .put(short1(randomValue2))
            .put(short0(randomValue2))
            .put(int2(counter))
            .put(int1(counter))
            .put(int0(counter))
    }

    private fun int3(x: Int): Byte = (x shr 24).toByte()

    private fun int2(x: Int): Byte = (x shr 16).toByte()

    private fun int1(x: Int): Byte = (x shr 8).toByte()

    private fun int0(x: Int): Byte = x.toByte()

    private fun short1(x: Short): Byte = (x.toInt() shr 8).toByte()

    private fun short0(x: Short): Byte = x.toByte()
}
