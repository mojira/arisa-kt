package io.github.mojira.arisa.modules.privacy

import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

private val TWO_SECONDS_AGO = RIGHT_NOW.minusSeconds(2)
private val TEN_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)

private const val MADE_PRIVATE_MESSAGE = "made-private"
private const val COMMENT_NOTE = "\n----\nRestricted by PrivacyModule ??[~arisabot]??"

private val NOOP_REDACTOR = object : AttachmentRedactor {
    override fun redact(attachment: Attachment): RedactedAttachment? {
        return null
    }
}

private fun createModule(
    message: String = MADE_PRIVATE_MESSAGE,
    commentNote: String = COMMENT_NOTE,
    allowedEmailRegexes: List<Regex> = emptyList(),
    sensitiveTextRegexes: List<Regex> = emptyList(),
    attachmentRedactor: AttachmentRedactor = NOOP_REDACTOR,
    sensitiveFileNameRegexes: List<Regex> = emptyList()
) = PrivacyModule(
    message,
    commentNote,
    allowedEmailRegexes,
    sensitiveTextRegexes,
    attachmentRedactor,
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

    context("attachment redaction") {
        // Example JWT token from https://jwt.io/
        val accessTokenText = "... --accessToken " +
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c" +
            " ..."
        val otherSensitiveText = "sensitive"
        val module = createModule(
            sensitiveTextRegexes = listOf(Regex.fromLiteral("--accessToken ey"), Regex.fromLiteral(otherSensitiveText)),
            attachmentRedactor = AccessTokenRedactor
        )

        test("should redact access tokens") {
            val uploader = "some-\n-user"
            val attachmentName = "my-\u202E-attachment.txt"

            var hasSetPrivate = false
            var hasDeletedAttachment = false
            var newAttachmentName: String? = null
            var newAttachmentContent: String? = null
            var addedComment: String? = null

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = attachmentName,
                        uploader = mockUser(
                            name = uploader
                        ),
                        getContent = { accessTokenText.toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addAttachmentFromFile = { file, cleanupCallback ->
                    newAttachmentName = file.name
                    newAttachmentContent = file.readText()
                    cleanupCallback()
                },
                addRawBotComment = { addedComment = it }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            // Attachment was redacted; issue should not have been made private
            hasSetPrivate shouldBe false
            hasDeletedAttachment shouldBe true
            newAttachmentName shouldBe "redacted_$attachmentName"
            newAttachmentContent shouldBe "... --accessToken ###REDACTED### ..."

            // Should also have sanitized user name and attachment name
            addedComment shouldBe "@[~some-?-user], sensitive data has been removed from your attachment(s) and they have " +
                "been re-uploaded as:\n" +
                "- [^redacted_my-?-attachment.txt]\n"
        }

        test("should add multiple comments when redacting attachments of multiple users") {
            val uploader1 = "some-user"
            val uploader1User = mockUser(
                name = uploader1
            )
            @Suppress("LocalVariableName")
            val attachmentName1_1 = "my-attachment.txt"
            @Suppress("LocalVariableName")
            val attachmentName1_2 = "my-attachment2.txt"

            val uploader2 = "some-other-user"
            val attachmentName2 = "other-attachment.txt"

            var hasSetPrivate = false
            val deletedAttachmentNames = mutableListOf<String>()
            val newAttachmentNames = mutableListOf<String>()
            val addedComments = mutableListOf<String>()

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = attachmentName1_1,
                        uploader = uploader1User,
                        getContent = { accessTokenText.toByteArray() },
                        remove = { deletedAttachmentNames.add(attachmentName1_1) }
                    ),
                    mockAttachment(
                        name = attachmentName1_2,
                        uploader = uploader1User,
                        getContent = { accessTokenText.toByteArray() },
                        remove = { deletedAttachmentNames.add(attachmentName1_2) }
                    ),
                    mockAttachment(
                        name = attachmentName2,
                        uploader = mockUser(name = uploader2),
                        getContent = { accessTokenText.toByteArray() },
                        remove = { deletedAttachmentNames.add(attachmentName2) }
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addAttachmentFromFile = { file, cleanupCallback ->
                    newAttachmentNames.add(file.name)
                    cleanupCallback()
                },
                addRawBotComment = { addedComments.add(it) }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            // Attachment was redacted; issue should not have been made private
            hasSetPrivate shouldBe false
            val expectedDeletedAttachmentNames = listOf(attachmentName1_1, attachmentName1_2, attachmentName2)
            deletedAttachmentNames shouldContainExactlyInAnyOrder expectedDeletedAttachmentNames
            newAttachmentNames shouldContainExactlyInAnyOrder expectedDeletedAttachmentNames.map { "redacted_$it" }

            addedComments shouldContainExactlyInAnyOrder listOf(
                "@[~some-user], sensitive data has been removed from your attachment(s) and they " +
                    "have been re-uploaded as:\n" +
                    "- [^redacted_my-attachment.txt]\n" +
                    "- [^redacted_my-attachment2.txt]\n",
                "@[~some-other-user], sensitive data has been removed from your attachment(s) " +
                    "and they have been re-uploaded as:\n" +
                    "- [^redacted_other-attachment.txt]\n"
            )
        }

        test("should not redact bot attachments") {
            var hasSetPrivate = false
            var hasDeletedAttachment = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        uploader = mockUser(
                            isBotUser = { true }
                        ),
                        getContent = { accessTokenText.toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            hasSetPrivate shouldBe true
            hasDeletedAttachment shouldBe false
        }

        test("should not redact attachments with non-redactable content") {
            var hasSetPrivate = false
            var hasDeletedAttachment = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        // Redactor does not handle this; but it represents sensitive data
                        getContent = { "some $otherSensitiveText and more text".toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            hasSetPrivate shouldBe true
            hasDeletedAttachment shouldBe false
        }

        test("should redact attachments even if issue is made private") {
            var hasSetPrivate = false
            var hasDeletedAttachment = false
            var hasAddedNewAttachment = false
            var privateComment: CommentOptions? = null
            var hasAddedRedactionComment = false

            val issue = mockIssue(
                // Issue description cannot be redacted (would still be in history)
                description = "some $otherSensitiveText and more text",
                attachments = listOf(
                    mockAttachment(
                        getContent = { accessTokenText.toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addAttachmentFromFile = { _, cleanupCallback ->
                    hasAddedNewAttachment = true
                    cleanupCallback()
                },
                addComment = { privateComment = it },
                addRawBotComment = { hasAddedRedactionComment = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            hasSetPrivate shouldBe true
            privateComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
            // Issue was made private, but attachment should have been redacted anyways
            hasDeletedAttachment shouldBe true
            hasAddedNewAttachment shouldBe true
            hasAddedRedactionComment shouldBe true
        }

        test("should make private if redacting does not remove all sensitive data") {
            var hasSetPrivate = false
            var hasDeletedAttachment = false
            var hasAddedNewAttachment = false
            var privateComment: CommentOptions? = null
            var hasAddedRedactionComment = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        // Other sensitive text cannot be redacted
                        getContent = { "$accessTokenText \n $otherSensitiveText".toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addAttachmentFromFile = { _, cleanupCallback ->
                    hasAddedNewAttachment = true
                    cleanupCallback()
                },
                addComment = { privateComment = it },
                addRawBotComment = { hasAddedRedactionComment = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            hasSetPrivate shouldBe true
            privateComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
            hasDeletedAttachment shouldBe false
            hasAddedNewAttachment shouldBe false
            hasAddedRedactionComment shouldBe false
        }

        test("should make private if attachment to redact has malformed name") {
            var hasSetPrivate = false
            var hasDeletedAttachment = false
            var hasAddedNewAttachment = false
            var privateComment: CommentOptions? = null
            var hasAddedRedactionComment = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        // Malformed name
                        name = "../../text.txt",
                        getContent = { accessTokenText.toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addAttachmentFromFile = { _, cleanupCallback ->
                    hasAddedNewAttachment = true
                    cleanupCallback()
                },
                addComment = { privateComment = it },
                addRawBotComment = { hasAddedRedactionComment = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            hasSetPrivate shouldBe true
            privateComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
            hasDeletedAttachment shouldBe false
            hasAddedNewAttachment shouldBe false
            hasAddedRedactionComment shouldBe false
        }

        test("should make private if attachment to redact has name clash") {
            val attachmentName = "text.txt"

            var hasSetPrivate = false
            var hasDeletedAttachment = false
            var hasAddedNewAttachment = false
            var privateComment: CommentOptions? = null
            var hasAddedRedactionComment = false

            val issue = mockIssue(
                attachments = listOf(
                    mockAttachment(
                        name = "redacted_$attachmentName"
                    ),
                    mockAttachment(
                        // Name would clash with other attachment name
                        name = attachmentName,
                        getContent = { accessTokenText.toByteArray() },
                        remove = { hasDeletedAttachment = true }
                    )
                ),
                setPrivate = { hasSetPrivate = true },
                addAttachmentFromFile = { _, cleanupCallback ->
                    hasAddedNewAttachment = true
                    cleanupCallback()
                },
                addComment = { privateComment = it },
                addRawBotComment = { hasAddedRedactionComment = true }
            )

            val result = module(issue, TWO_SECONDS_AGO)
            result.shouldBeRight(ModuleResponse)

            hasSetPrivate shouldBe true
            privateComment shouldBe CommentOptions(MADE_PRIVATE_MESSAGE)
            hasDeletedAttachment shouldBe false
            hasAddedNewAttachment shouldBe false
            hasAddedRedactionComment shouldBe false
        }
    }
})
