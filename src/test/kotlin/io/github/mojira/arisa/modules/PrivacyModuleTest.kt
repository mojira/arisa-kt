package io.github.mojira.arisa.modules

import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val TEN_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

private const val MADE_PRIVATE_MESSAGE = "made-private"
private const val COMMENT_NOTE = "\n----\nRestricted by PrivacyModule ??[~arisabot]??"

private fun createModule(
    message: String = MADE_PRIVATE_MESSAGE,
    commentNote: String = COMMENT_NOTE,
    allowedEmailRegexes: List<Regex> = emptyList(),
    sensitiveTextRegexes: List<Regex> = emptyList(),
    sensitiveFileNameRegexes: List<Regex> = emptyList()
) = PrivacyModule(
    message,
    commentNote,
    allowedEmailRegexes,
    sensitiveTextRegexes,
    sensitiveFileNameRegexes
)

class PrivacyModuleTest : FunSpec({
    data class ContentTestData(
        val module: PrivacyModule,
        val sensitiveText: String,
        val testType: String
    )

    // Shared tests for email address and sensitive text detection
    listOf(
        ContentTestData(
            createModule(),
            "some text and test@example.com and \n another line",
            "email"
        ),
        ContentTestData(
            createModule(sensitiveTextRegexes = listOf(Regex.fromLiteral("sensitive"))),
            "some sensitive text and \n another line",
            "sensitive text"
        )
    ).forEach { testData ->
        context(testData.testType) {
            context("ticket") {
                test("should return OperationNotNeededModuleResponse when the ticket is marked as private") {
                    val issue = mockIssue(
                        securityLevel = "private",
                        summary = testData.sensitiveText,
                        environment = testData.sensitiveText,
                        description = testData.sensitiveText,
                        attachments = listOf(
                            mockAttachment(
                                name = testData.sensitiveText,
                                getContent = { testData.sensitiveText.toByteArray() }
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the ticket is created before lastRun") {
                    val issue = mockIssue(
                        created = TEN_SECONDS_AGO,
                        description = testData.sensitiveText
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the attachment is created before lastRun") {
                    val issue = mockIssue(
                        attachments = listOf(
                            mockAttachment(
                                created = TEN_SECONDS_AGO,
                                getContent = { testData.sensitiveText.toByteArray() }
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the attachment is not a text file") {
                    val issue = mockIssue(
                        attachments = listOf(
                            mockAttachment(
                                mimeType = "image/png",
                                getContent = { testData.sensitiveText.toByteArray() }
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the change log item is created before lastRun") {
                    val issue = mockIssue(
                        changeLog = listOf(
                            mockChangeLogItem(
                                created = TEN_SECONDS_AGO,
                                changedFromString = null,
                                changedToString = testData.sensitiveText
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the ticket does not contain sensitive data") {
                    val issue = mockIssue(
                        summary = "Test"
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should mark as private when the attachment contains sensitive data") {
                    var hasSetPrivate = false
                    var addedComment: CommentOptions? = null

                    val issue = mockIssue(
                        attachments = listOf(
                            mockAttachment(
                                getContent = { testData.sensitiveText.toByteArray() }
                            )
                        ),
                        setPrivate = { hasSetPrivate = true },
                        addComment = { addedComment = it }
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeRight(ModuleResponse)
                    hasSetPrivate shouldBe true
                    addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
                }

                test("should mark as private when the summary contains sensitive data") {
                    var hasSetPrivate = false
                    var addedComment: CommentOptions? = null

                    val issue = mockIssue(
                        summary = testData.sensitiveText,
                        setPrivate = { hasSetPrivate = true },
                        addComment = { addedComment = it }
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeRight(ModuleResponse)
                    hasSetPrivate shouldBe true
                    addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
                }

                test("should mark as private when the environment contains sensitive data") {
                    var hasSetPrivate = false
                    var addedComment: CommentOptions? = null

                    val issue = mockIssue(
                        environment = testData.sensitiveText,
                        setPrivate = { hasSetPrivate = true },
                        addComment = { addedComment = it }
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeRight(ModuleResponse)
                    hasSetPrivate shouldBe true
                    addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
                }

                test("should mark as private when the description contains sensitive data") {
                    var hasSetPrivate = false
                    var addedComment: CommentOptions? = null

                    val issue = mockIssue(
                        description = testData.sensitiveText,
                        setPrivate = { hasSetPrivate = true },
                        addComment = { addedComment = it }
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeRight(ModuleResponse)
                    hasSetPrivate shouldBe true
                    addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
                }

                test("should mark as private when the change log item contains sensitive data") {
                    var hasSetPrivate = false
                    var addedComment: CommentOptions? = null

                    val issue = mockIssue(
                        changeLog = listOf(
                            mockChangeLogItem(
                                field = "environment",
                                changedFromString = null,
                                changedToString = testData.sensitiveText
                            )
                        ),
                        setPrivate = { hasSetPrivate = true },
                        addComment = { addedComment = it }
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeRight(ModuleResponse)
                    hasSetPrivate shouldBe true
                    addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
                }
            }

            context("comment") {
                test("should return OperationNotNeededModuleResponse when the ticket is marked as private") {
                    val issue = mockIssue(
                        securityLevel = "private",
                        comments = listOf(
                            mockComment(
                                body = testData.sensitiveText
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the comment is created before lastRun") {
                    val issue = mockIssue(
                        comments = listOf(
                            mockComment(
                                body = testData.sensitiveText,
                                created = TEN_SECONDS_AGO
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should return OperationNotNeededModuleResponse when the comment is not public") {
                    val issue = mockIssue(
                        comments = listOf(
                            mockComment(
                                body = testData.sensitiveText,
                                visibilityType = "group",
                                visibilityValue = "helper"
                            )
                        )
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeLeft(OperationNotNeededModuleResponse)
                }

                test("should restrict to staff when the comment contains sensitive data") {
                    var hasSetPrivate = false
                    var hasRestrictedComment = false

                    val issue = mockIssue(
                        comments = listOf(
                            mockComment(
                                body = testData.sensitiveText,
                                restrict = {
                                    hasRestrictedComment = true
                                    it shouldBe "${testData.sensitiveText}$COMMENT_NOTE"
                                }
                            )
                        ),
                        setPrivate = { hasSetPrivate = true }
                    )

                    val result = testData.module(issue, TWO_SECONDS_AGO)

                    result.shouldBeRight(ModuleResponse)
                    hasSetPrivate shouldBe false
                    hasRestrictedComment shouldBe true
                }
            }
        }
    }

    // Tests specific to email address detection, which cannot be shared with 'sensitive text' tests above
    context("email (specific)") {
        test("should return OperationNotNeededModuleResponse when the email address is contained in a user mention") {
            val issue = mockIssue(
                summary = "[~foo@example.com]"
            )

            val result = createModule()(issue, TWO_SECONDS_AGO)

            result.shouldBeLeft(OperationNotNeededModuleResponse)
        }

        test("should mark as private when the email is contained in a link") {
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                description = "[foo@example.com|mailto:foo@example.com]",
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = createModule()(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }

        test("should mark as private when the email address contains dots") {
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                summary = "f.o.o@example.com",
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = createModule()(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }

        test("should mark as private when the email address uses .cc tld") {
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                summary = "foo@example.cc",
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = createModule()(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }

        test("should mark as private when the email address uses .americanexpress tld") {
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                summary = "foo@example.americanexpress",
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = createModule()(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }

        test("should not mark as private when the change log item contains an allowed email") {
            val allowedEmail = "allowed@example.com"
            val allowedEmailRegexes = listOf(Regex.fromLiteral(allowedEmail))
            val module = createModule(allowedEmailRegexes = allowedEmailRegexes)
            var hasSetPrivate = false

            val issue = mockIssue(
                changeLog = listOf(
                    mockChangeLogItem(
                        field = "environment",
                        changedFromString = null,
                        changedToString = "My email is $allowedEmail."
                    )
                ),
                setPrivate = { hasSetPrivate = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)

            result.shouldBeLeft(OperationNotNeededModuleResponse)
            hasSetPrivate shouldBe false
        }

        test("should mark as private when the change log item contains an allowed email and a not allowed email") {
            val allowedEmail = "allowed@example.com"
            val allowedEmailRegexes = listOf(Regex.fromLiteral(allowedEmail))
            val module = createModule(allowedEmailRegexes = allowedEmailRegexes)
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                changeLog = listOf(
                    mockChangeLogItem(
                        field = "environment",
                        changedFromString = null,
                        changedToString = "My email is $allowedEmail but I also use foo@example.com."
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = module(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }
    }

    context("sensitive file names") {
            val sensitiveFileName = "sensitive.txt"
            val module = createModule(sensitiveFileNameRegexes = listOf(Regex.fromLiteral(sensitiveFileName)))

        test("should not mark as private when attachment has been created before last run") {
            var hasSetPrivate = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = sensitiveFileName,
                        created = TEN_SECONDS_AGO
                    )
                ),
                setPrivate = { hasSetPrivate = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)

            result.shouldBeLeft(OperationNotNeededModuleResponse)
            hasSetPrivate shouldBe false
        }

        test("should not mark as private when attachment has non-sensitive name") {
            var hasSetPrivate = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = "non-$sensitiveFileName"
                    )
                ),
                setPrivate = { hasSetPrivate = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)

            result.shouldBeLeft(OperationNotNeededModuleResponse)
            hasSetPrivate shouldBe false
        }

        test("should mark as private when attachment has sensitive name") {
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = sensitiveFileName
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = module(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }

        test("should mark as private when non-text attachment has sensitive name") {
            var hasSetPrivate = false
            var addedComment: CommentOptions? = null

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = sensitiveFileName,
                        mimeType = "image/png"
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addComment = { addedComment = it }
            )

            val result = module(issue, TWO_SECONDS_AGO)

            result.shouldBeRight(ModuleResponse)
            hasSetPrivate shouldBe true
            addedComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
        }
    }
})
