package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val TEN_SECONDS_LATER = RIGHT_NOW.plusSeconds(10)
private val TEN_THOUSAND_YEARS_LATER = RIGHT_NOW.plusSeconds(315360000000)

private const val BOT_SIGNATURE_KEY = "bot-signature"

class DuplicateMessageModuleTest : StringSpec({
    val module = DuplicateMessageModule(
        0L,
        "duplicate",
        BOT_SIGNATURE_KEY,
        "duplicate-forward",
        mapOf("MC-297" to "duplicate-of-mc-297"),
        "duplicate-private",
        listOf("ARISA_NO_DUPLICATE_MESSAGE"),
        mapOf("Fixed" to "duplicate-fixed")
    )

    "should return OperationNotNeededModuleResponse when the issue has no change log" {
        val issue = getIssue(
            changeLog = emptyList()
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no link change log" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Confirmation Status",
                    changedToString = "Confirmed"
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no duplicate link change log" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    field = "Link",
                    changedTo = "MC-42",
                    changedToString = "This issue relates to MC-42"
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue was linked before last run" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TWO_SECONDS_AGO,
                    field = "Link",
                    changedTo = "MC-42",
                    changedToString = "This issue duplicates MC-42"
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no links" {
        val issue = getIssue()

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no duplicate links" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-42",
                    changedToString = "This issue relates to MC-42"
                )
            ),
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
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-42",
                    changedToString = "This issue is duplicated by MC-42"
                )
            ),
            links = listOf(
                mockLink(
                    outwards = false
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent has been mentioned in a public comment by staff" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
            links = listOf(
                mockLink()
            ),
            comments = listOf(
                mockComment(
                    body = "This duplicates MC-1.",
                    getAuthorGroups = { listOf("staff") }
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the ticket has a prevent message tag" {
        val issue = getIssue(
                changeLog = listOf(
                        mockChangeLogItem(
                                created = TEN_THOUSAND_YEARS_LATER,
                                field = "Link",
                                changedTo = "MC-1",
                                changedToString = "This issue duplicates MC-1"
                        )
                ),
                links = listOf(
                        mockLink()
                ),
                comments = listOf(
                        mockComment(
                                body = "ARISA_NO_DUPLICATE_MESSAGE",
                                visibilityType = "group",
                                visibilityValue = "staff"
                        )
                )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when all the parents have been mentioned in a public comment by staff" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
                    body = "This duplicates MC-1 and MC-2.",
                    getAuthorGroups = { listOf("staff") }
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse even if only a portion of parents have been mentioned by staff" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
                    body = "This duplicates MC-1.",
                    getAuthorGroups = { listOf("staff") }
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when all the parents have been mentioned in different public comments by staff" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
                    body = "This duplicates MC-1.",
                    getAuthorGroups = { listOf("staff") }
                ),
                mockComment(
                    body = "This duplicates MC-2.",
                    getAuthorGroups = { listOf("staff") }
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when an historical parent has been mentioned in comments by staff" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-2"
                    )
                )
            ),
            comments = listOf(
                mockComment(
                    body = "This duplicates MC-1.",
                    getAuthorGroups = { listOf("staff") }
                )
            )
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should add comment when the parent hasn't been mentioned anywhere" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
            links = listOf(
                mockLink()
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add comment when the parent has only been mentioned in an restricted comment" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add comment when the parent has been mentioned in a comment by a user not in a group" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
            links = listOf(
                mockLink()
            ),
            comments = listOf(
                mockComment(
                    body = "MC-1"
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add comment when the parent has been mentioned in a comment by a normal user" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
            links = listOf(
                mockLink()
            ),
            comments = listOf(
                mockComment(
                    body = "MC-1",
                    getAuthorGroups = { listOf("user") }
                )
            ),
            addComment = { commentOptions = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add comment with all three parents' keys in ascending order" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-3",
                    changedToString = "This issue duplicates MC-3"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1*, *MC-2*, and *MC-3",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for specific ticket when there's only one parent" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-297",
                    changedToString = "This issue duplicates MC-297"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-of-mc-297",
            "MC-297",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the normal comment even if one of the parents is a special ticket" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-297",
                    changedToString = "This issue duplicates MC-297"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1* and *MC-297",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for private parent when the only parent is private" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-private",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the normal comment for private parent when the parent is private but the reporters are identical" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            reporter = "Arisa",
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                reporter = mockUser(
                                    "Arisa"
                                ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for private parents when all parents are private" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-private",
            "MC-1* and *MC-2",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the normal comment even if portion of the parents are private" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1* and *MC-2",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for specific resolution when the only parent has that resolution" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-fixed",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for specific resolution when all parents have the same resolution" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-fixed",
            "MC-1* and *MC-2",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the normal comment even if portion of the parents have the special resolution" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1* and *MC-2",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the normal comment if there's no special message for the resolution" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for specific parent instead of the comment for private parents" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-297",
                    changedToString = "This issue duplicates MC-297"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-of-mc-297",
            "MC-297",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should add the comment for private instead of the comment for specific resolution" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
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
        commentOptions shouldBe CommentOptions(
            "duplicate-private",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
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
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                ),
                mockChangeLogItem(
                    created = TEN_THOUSAND_YEARS_LATER,
                    field = "Link",
                    changedTo = "MC-2",
                    changedToString = "This issue duplicates MC-2"
                )
            ),
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

    "should add the forward comment if the ticket was created before the parent" {
        var commentOptions: CommentOptions? = null
        val issue = getIssue(
            changeLog = listOf(
                mockChangeLogItem(
                    created = TEN_SECONDS_LATER,
                    field = "Link",
                    changedTo = "MC-1",
                    changedToString = "This issue duplicates MC-1"
                )
            ),
            links = listOf(
                mockLink(
                    issue = mockLinkedIssue(
                        key = "MC-1",
                        getFullIssue = {
                            mockIssue(
                                    created = TEN_THOUSAND_YEARS_LATER,
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
        commentOptions shouldBe CommentOptions(
            "duplicate-forward",
            "MC-1",
            signatureMessageKey = BOT_SIGNATURE_KEY
        )
    }
})

private fun getIssue(
    reporter: String = "User",
    links: List<Link> = emptyList(),
    changeLog: List<ChangeLogItem> = listOf(
        mockChangeLogItem(
            created = TEN_THOUSAND_YEARS_LATER,
            field = "Link",
            changedTo = "MC-42",
            changedToString = "This issue duplicates MC-42"
        )
    ),
    comments: List<Comment> = emptyList(),
    addComment: (options: CommentOptions) -> Unit = { }
) = mockIssue(
    reporter = mockUser(
        reporter
    ),
    links = links,
    changeLog = changeLog,
    comments = comments,
    addComment = addComment
)
