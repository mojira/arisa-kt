package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

const val DESC_DEFAULT = """Put the summary of the bug you're having here

*What I expected to happen was...:*
Describe what you thought should happen here

*What actually happened was...:*
Describe what happened here

*Steps to Reproduce:*
1. Put a step by step guide on how to trigger the bug here
2. ...
3. ..."""
const val ENV_DEFAULT = "Put your operating system (Windows 7, Windows XP, OSX) and Java version if you know it here"
const val MIN_LENGTH = 5

class EmptyModule(
    private val message: String
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertAfter(created, lastRun).bind()
            if (description != DESC_DEFAULT && environment != ENV_DEFAULT) {
                assertNotBigger(description, MIN_LENGTH).bind()
                assertNotBigger(environment, MIN_LENGTH).bind()
            } else {
                assertNotEqual(description, DESC_DEFAULT).bind()
                assertNotEqual(environment, ENV_DEFAULT).bind()
            }
            assertEmpty(attachments).bind()
            resolution = "Incomplete"
            addComment(message)
        }
    }

    private fun assertNotBigger(s: String?, size: Int) = when {
        s.isNullOrBlank() -> Unit.right()
        s.length > size -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertNotEqual(s: String?, default: String) = when {
        s.isNullOrBlank() -> Unit.right()
        s != default -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
