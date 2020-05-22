package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val TEN_THOUSAND_YEARS_LATER = RIGHT_NOW.plusSeconds(315360000000)

class DuplicateMessageModuleTest : StringSpec({
    val module = DuplicateMessageModule(
        "duplicate",
        mapOf("MC-297" to "duplicate-of-mc-297"),
        "duplicate-private",
        mapOf("Fixed" to "duplicate-fixed")
    )

    "should return OperationNotNeededModuleResponse when the issue has no resolved time" {
        val issue = mockIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue was resolved before last run" {
        val issue = mockIssue(
            resolved = TWO_SECONDS_AGO
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no links" {
        val issue = mockIssue(
            resolved = RIGHT_NOW.plusSeconds(3)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no duplicate links" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    type = "Relates"
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no outward duplicate links" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    outwards = false
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent has been mentioned in a public comment" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink()
            ),
            comments = listOf(
                mockComment(
                    body = "This duplicates MC-1."
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when all the parents have been mentioned in a public comment" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2"
                    )
                )
            ),
            comments = listOf(
                mockComment(
                    body = "This duplicates MC-1 and MC-2."
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse even if only a portion of parents have been mentioned" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2"
                    )
                )
            ),
            comments = listOf(
                mockComment(
                    body = "This duplicates MC-1."
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when all the parents have been mentioned in different public comments" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2"
                    )
                )
            ),
            comments = listOf(
                mockComment(
                    body = "This duplicates MC-1."
                ),
                mockComment(
                    body = "This duplicates MC-2."
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should add comment when the parent hasn't been mentioned anywhere" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink()
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1")
    }

    "should add comment when the parent has only been mentioned in an restricted comment" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink()
            ),
            comments = listOf(
                mockComment(
                    body = "MC-1",
                    visibilityType = "group",
                    visibilityValue = "helper"
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1")
    }

    "should add comment with all three parents' keys in ascending order" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-3"
                    )
                ),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2"
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1*, *MC-2*, and *MC-3")
    }

    "should add the comment for specific ticket when there's only one parent" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-297"
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-of-mc-297", "MC-297")
    }

    "should add the normal comment even if one of the parents is a special ticket" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-297"
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1* and *MC-297")
    }

    "should add the comment for private parent when the only parent is private" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-private", "MC-1")
    }

    "should add the comment for private parents when all parents are private" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private"
                            ).right()
                        }
                    )
                ),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-private", "MC-1* and *MC-2")
    }

    "should add the normal comment even if portion of the parents are private" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1* and *MC-2")
    }

    "should add the normal comment if there's no special message for private parents" {
        val module = DuplicateMessageModule(
            "duplicate",
            mapOf("MC-297" to "duplicate-of-mc-297"),
            null,
            mapOf("Fixed" to "duplicate-fixed")
        )
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1")
    }

    "should add the comment for specific resolution when the only parent has that resolution" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                resolution = "Fixed"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-fixed", "MC-1")
    }

    "should add the comment for specific resolution when all parents have the same resolution" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                resolution = "Fixed"
                            ).right()
                        }
                    )
                ),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2",
                        getFullIssue = {
                            mockIssue(
                                resolution = "Fixed"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-fixed", "MC-1* and *MC-2")
    }

    "should add the normal comment even if portion of the parents have the special resolution" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2",
                        getFullIssue = {
                            mockIssue(
                                resolution = "Fixed"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1* and *MC-2")
    }

    "should add the normal comment if there's no special message for the resolution" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                resolution = "Invalid"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate", "MC-1")
    }

    "should add the comment for specific parent instead of the comment for private parents" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-297",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-of-mc-297", "MC-297")
    }

    "should add the comment for private instead of the comment for specific resolution" {
        var commentOptions: CommentOptions? = null
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                securityLevel = "private",
                                resolution = "Fixed"
                            ).right()
                        }
                    )
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions("duplicate-private", "MC-1")
    }

    "should return FailedModuleResponse when adding comments fails" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink()
            ),
            addComment = { RuntimeException().left() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        getFullIssue = { RuntimeException().left() }
                    )
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val issue = mockIssue(
            resolved = TEN_THOUSAND_YEARS_LATER,
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = { RuntimeException().left() }
                    )
                ),
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2",
                        getFullIssue = { RuntimeException().left() }
                    )
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})
