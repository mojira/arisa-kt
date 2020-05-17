package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class KeepPrivateModuleTest : StringSpec({
    val REMOVE_SECURITY = ChangeLogItem(NOW.minusSeconds(10), "security", "10318", null) { emptyList() }

    "should return OperationNotNeededModuleResponse when keep private tag is null" {
        val module = KeepPrivateModule(null)
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(null, "private", listOf(comment), listOf(REMOVE_SECURITY), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comments are empty" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", emptyList(), listOf(REMOVE_SECURITY), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no comment contains private tag" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("Hello world!", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(null, "private", listOf(comment), listOf(REMOVE_SECURITY), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment isn't restricted to staff group" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", listOf(comment), listOf(REMOVE_SECURITY), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when security level is set to private" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request("private", "private", listOf(comment), emptyList(), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to private when security level is null" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(null, "private", listOf(comment), listOf(REMOVE_SECURITY), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should both set to private and comment when security level is not private" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(
            "not private", "private", listOf(comment), listOf(REMOVE_SECURITY),
            { didSetToPrivate = true; Unit.right() },
            { didComment = true; Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should set to private but not comment when security level has neven been changed" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", NOW.minusSeconds(2), "group", "staff")
        val request = KeepPrivateModule.Request(
            null, "private", listOf(comment), listOf(REMOVE_SECURITY),
            { didSetToPrivate = true; Unit.right() },
            { didComment = true; Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe false
    }

    "should set to private but not comment when security level has neven been changed since the ticket was marked" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(
            null, "private", listOf(comment), emptyList(),
            { didSetToPrivate = true; Unit.right() },
            { didComment = true; Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe false
    }

    "should return FailedModuleResponse when setting security level fails" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(null, "private", listOf(comment), listOf(REMOVE_SECURITY), { RuntimeException().left() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when posting comment" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val comment = getComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val request = KeepPrivateModule.Request(null, "private", listOf(comment), listOf(REMOVE_SECURITY), { Unit.right() }, { RuntimeException().left() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getComment(
    body: String = "",
    created: Instant = NOW.minusSeconds(100),
    visibilityType: String? = null,
    visibilityValue: String? = null
) = Comment(
    body,
    "",
    { null },
    created,
    created,
    visibilityType,
    visibilityValue,
    { Unit.right() },
    { Unit.right() })
