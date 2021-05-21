package io.github.mojira.arisa.modules

import java.io.IOException
import java.io.InputStream
import java.lang.Long.min

/**
 * [InputStream] which reads at most a certain number of bytes.
 */
class LimitingInputStream(private val stream: InputStream, private val maxBytes: Long) : InputStream() {
    private var totalReadAmount: Long = 0

    private fun getRemaining(): Long = maxBytes - totalReadAmount

    private fun getMaxReadAmount(desiredReadAmount: Int): Int {
        if (desiredReadAmount <= 0) {
            return 0
        }

        val remaining = getRemaining()
        if (remaining <= 0) {
            throw IOException("Trying to read more than $maxBytes bytes")
        }
        // Conversion to Int here is safe because result will be at most desiredReadAmount (= Int)
        return min(remaining, desiredReadAmount.toLong()).toInt()
    }

    override fun read(): Int {
        getMaxReadAmount(1) // Check that reading 1 byte is allowed
        val result = stream.read()
        if (result != -1) {
            totalReadAmount++
        }
        return result
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val actualLen = getMaxReadAmount(len)
        val readAmount = stream.read(b, off, actualLen)
        if (readAmount != -1) {
            totalReadAmount += readAmount
        }
        return readAmount
    }

    override fun available(): Int {
        // Conversion to Int here is safe because result will be at most stream.available() (= Int)
        return min(stream.available().toLong(), getRemaining()).toInt()
    }

    override fun close() {
        stream.close()
    }
}
