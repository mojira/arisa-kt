package io.github.mojira.arisa.modules.thumbnail

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.IOException

class LimitingInputStreamTest : StringSpec({
    "should close delegate" {
        val limit = 16
        var wasDelegateClosed = false
        val delegate = object : ByteArrayInputStream(ByteArray(limit)) {
            override fun close() {
                wasDelegateClosed = true
            }
        }
        val stream = LimitingInputStream(delegate, limit.toLong())
        stream.read()

        wasDelegateClosed shouldBe false
        stream.close()
        wasDelegateClosed shouldBe true
    }

    "should not throw exception when limit is higher than bytes count" {
        val limit = 16
        val bytesCount = limit / 2

        val delegate = ByteArrayInputStream(ByteArray(bytesCount) { i -> i.toByte() })
        val stream = LimitingInputStream(delegate, limit.toLong())
        stream.available() shouldBe bytesCount

        // Consume all bytes
        for (i in 1..bytesCount) {
            stream.read() shouldBe i - 1
            stream.available() shouldBe (bytesCount - i)
        }

        stream.read() shouldBe -1
    }

    "should throw exception when limit is reached" {
        val limit = 16

        var wasDelegateClosed = false
        val delegate = object : ByteArrayInputStream(ByteArray(limit * 2) { i -> i.toByte() }) {
            override fun close() {
                wasDelegateClosed = true
            }
        }

        val stream = LimitingInputStream(delegate, limit.toLong())
        stream.available() shouldBe limit

        // Consume all allowed bytes
        for (i in 1..limit) {
            stream.read() shouldBe i - 1
            stream.available() shouldBe (limit - i)
        }

        val expectedMessage = "Trying to read more than $limit bytes"
        shouldThrow<IOException> { stream.read() }.message shouldBe expectedMessage
        shouldThrow<IOException> { stream.readAllBytes() }.message shouldBe expectedMessage
        shouldThrow<IOException> { stream.read(ByteArray(10)) }.message shouldBe expectedMessage
        shouldThrow<IOException> { stream.skip(10) }.message shouldBe expectedMessage

        wasDelegateClosed shouldBe false
        stream.close()
        wasDelegateClosed shouldBe true
    }
})
