package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.modules.HideImpostorsModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no comments" {
        val module = HideImpostorsModule()
        val request = Request(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesnt contain [ but contains ]" {
        val module = HideImpostorsModule()
        val comment = Comment("test] test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesnt contain ] but contains [" {
        val module = HideImpostorsModule()
        val comment = Comment("[test test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains invalid characters in the tag" {
        val module = HideImpostorsModule()
        val comment = Comment("[}[{]] test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user is only the tag" {
        val module = HideImpostorsModule()
        val comment = Comment("[test]", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when tag is not at the beginning" {
        val module = HideImpostorsModule()
        val comment = Comment("test [test]", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group staff" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { listOf("staff") }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group helper" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { listOf("helper") }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group global-moderators" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { listOf("global-moderators") }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is hidden" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { emptyList() }, Instant.now(), "group", "staff") { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is more than a day old" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { listOf("staff") }, Instant.now().minus(2, ChronoUnit.DAYS), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should hide comment when user starts with a valid tag but is not of a permission group" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when tag contains numbers" {
        val module = HideImpostorsModule()
        val comment = Comment("[t3st] test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when tag contains accented letter" {
        val module = HideImpostorsModule()
        val comment = Comment("[tést] test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when tag contains spaces" {
        val module = HideImpostorsModule()
        val comment = Comment("[Mojang Overlord] test", { emptyList() }, Instant.now(), null, null) { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when user contains [] but is not of a permission group and comment is not restricted to a group" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { emptyList() }, Instant.now(), "not a group", "staff") { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should hide comment when user contains [] but is not of a permission group and comment is not restricted to the correct group" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { emptyList() }, Instant.now(), "group", "users") { Unit.right() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when hiding the comment fails" {
        val module = HideImpostorsModule()
        val comment = Comment("[test] test", { emptyList() }, Instant.now(), null, null) { RuntimeException().left() }
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
