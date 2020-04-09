package io.github.mojira.arisa.modules

import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.ChangeLogEntry
import net.rcarz.jiraclient.ChangeLogItem
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.User
import net.rcarz.jiraclient.Visibility
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class HideImpostorsTest : StringSpec({

    "should return OperationNotNeededModuleResponse when no comments" {
        val module = HideImpostorsModule({ emptyList<String>().right() }, { _: Comment, _: String -> Unit.right() })
        val request = HideImpostorsModuleRequest(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesnt contain [ but contains ]" {
        val module = HideImpostorsModule({ emptyList<String>().right() }, { _: Comment, _: String -> Unit.right() })
        val comment = mockComment("test]")
        val request = HideImpostorsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user doesnt contain ] but contains [" {
        val module = HideImpostorsModule({ emptyList<String>().right() }, { _: Comment, _: String -> Unit.right() })
        val comment = mockComment("[test")
        val request = HideImpostorsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but has group staff" {
        val module = HideImpostorsModule({ listOf("staff").right() }, { _: Comment, _: String -> Unit.right() })
        val comment = mockComment("[test] test")
        val request = HideImpostorsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is hidden" {
        val module = HideImpostorsModule({ emptyList<String>().right() }, { _: Comment, _: String -> Unit.right() })
        val visibility = mockVisibility("group", "staff")
        val comment = mockComment("[test] test", visibility)
        val request = HideImpostorsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when user contains [] but is not staff and comment is more than a day old" {
        val module = HideImpostorsModule({ emptyList<String>().right() }, { _: Comment, _: String -> Unit.right() })
        val comment = mockComment("[test] test", date = Date.from(Instant.now().minus(2, ChronoUnit.DAYS)))
        val request = HideImpostorsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should hide comment when user contains [] but is not staff" {
        val module = HideImpostorsModule({ emptyList<String>().right() }, { _: Comment, _: String -> Unit.right() })
        val comment = mockComment("[test] test")
        val request = HideImpostorsModuleRequest(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})

private fun mockComment(author: String, visibility: Visibility? = null, date: Date = Date()): Comment {
    val comment = mockk<Comment>()
    val user = mockk<User>()
    every { user.displayName } returns author
    every { user.name } returns author
    every { comment.author } returns user
    every { comment.body } returns "Test"
    every { comment.visibility } returns visibility
    every { comment.updatedDate } returns date
    return comment
}

private fun mockChangelogEntry(authorName: String, fieldName: String, fieldValue: String): ChangeLogEntry {
    val changeLogEntry = mockk<ChangeLogEntry>()
    every { changeLogEntry.author.name } returns authorName
    every { changeLogEntry.items } returns listOf(mockChangelogItem(fieldName, fieldValue))

    return changeLogEntry
}

private fun mockChangelogItem(name: String, value: String): ChangeLogItem {
    val changeLogItem = mockk<ChangeLogItem>()
    every { changeLogItem.field } returns name
    every { changeLogItem.toString } returns value

    return changeLogItem
}

private fun mockVisibility(type: String, value: String): Visibility {
    val visibility = mockk<Visibility>()
    every { visibility.type } returns type
    every { visibility.value } returns value
    return visibility
}
