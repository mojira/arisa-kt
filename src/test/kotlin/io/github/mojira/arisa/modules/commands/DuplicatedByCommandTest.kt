package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DuplicatedByCommandTest : StringSpec({
    "should return OperationNotNeededModuleResponse when no argument is passed" {
        val command = DuplicatedByCommand()

        val result = command(mockIssue(), "ARISA_DUPLICATED_BY")
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse in Either when given not a ticket" {
        val command = DuplicatedByCommand()

        val result = command(mockIssue(), "ARISA_DUPLICATED_BY", "MC-1", "https://bugs.mojang.com/browse/MC-2", "lalala", "MC-3")
        result shouldBeLeft OperationNotNeededModuleResponse
    }

    "should resolve as duplicate and link to parent ticket" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val list = listOf(
            mockIssue(
                key = "MC-100",
                resolution = "Unresolved",
                resolveAsDuplicate = {
                    resolvedList.add("MC-100")
                },
                createLink = {
                    type, key, outwards ->
                    linkMap["MC-100"] = listOf(type, key, outwards.toString())
                }
            ),
            mockIssue(
                key = "MC-200",
                resolution = "Unresolved",
                resolveAsDuplicate = {
                    resolvedList.add("MC-200")
                },
                createLink = {
                    type, key, outwards ->
                    linkMap["MC-200"] = listOf(type, key, outwards.toString())
                }
            )
        )
        val issue = mockIssue(
            key = "MC-1",
            getOtherIssue = {
                ticket -> list.first {
                    it.key == ticket
                }.right()
            }
        )

        command(issue, "ARISA_DUPLICATED_BY", "MC-100") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-1", "true")))
        resolvedList.clear()
        linkMap.clear()

        command(issue, "ARISA_DUPLICATED_BY", "MC-100", "MC-200") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100", "MC-200"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-1", "true"), "MC-200" to listOf("Duplicate", "MC-1", "true")))
    }

    "should support urls" {
        val command = DuplicatedByCommand()
        var resolvedVar = ""
        val linkList = mutableListOf<String>()
        val list = listOf(
                mockIssue(
                        key = "MC-100",
                        resolution = "Unresolved",
                        resolveAsDuplicate = {
                            resolvedVar = "MC-100"
                        },
                        createLink = {
                            type, key, outwards ->
                            linkList.addAll(listOf(type, key, outwards.toString()))
                        }
                )
        )
        val issue = mockIssue(
                key = "MC-1",
                getOtherIssue = {
                    ticket -> list.first {
                        it.key == ticket
                    }.right()
                }
        )

        command(issue, "ARISA_DUPLICATED_BY", "https://bugs.mojang.com/browse/MC-100") shouldBeRight ModuleResponse
        resolvedVar shouldBe("MC-100")
        linkList shouldBe(mutableListOf("Duplicate", "MC-1", "true"))
    }

    "should support commas" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val list = listOf(
                mockIssue(
                        key = "MC-100",
                        resolution = "Unresolved",
                        resolveAsDuplicate = {
                            resolvedList.add("MC-100")
                        },
                        createLink = {
                            type, key, outwards ->
                            linkMap["MC-100"] = listOf(type, key, outwards.toString())
                        }
                ),
                mockIssue(
                        key = "MC-200",
                        resolution = "Unresolved",
                        resolveAsDuplicate = {
                            resolvedList.add("MC-200")
                        },
                        createLink = {
                            type, key, outwards ->
                            linkMap["MC-200"] = listOf(type, key, outwards.toString())
                        }
                )
        )
        val issue = mockIssue(
                key = "MC-1",
                getOtherIssue = {
                    ticket -> list.first {
                    it.key == ticket
                }.right()
                }
        )

        command(issue, "ARISA_DUPLICATED_BY", "MC-100,", "MC-200") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100", "MC-200"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-1", "true"), "MC-200" to listOf("Duplicate", "MC-1", "true")))
        resolvedList.clear()
        linkMap.clear()

        command(issue, "ARISA_DUPLICATED_BY", "MC-100,MC-200") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100", "MC-200"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-1", "true"), "MC-200" to listOf("Duplicate", "MC-1", "true")))
    }

    "should ignore unreachable/nonexistent keys" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val throwable = Throwable("fail")
        val child = mockIssue(
                key = "MC-100",
                resolution = "Unresolved",
                resolveAsDuplicate = {
                    resolvedList.add("MC-100")
                },
                createLink = {
                    type, key, outwards ->
                    linkMap["MC-100"] = listOf(type, key, outwards.toString())
                }
        )
        val issue = mockIssue(
                key = "MC-1",
                getOtherIssue = {
                    ticket -> if (ticket == "MC-100") child.right() else throwable.left()
                }
        )

        command(issue, "ARISA_DUPLICATED_BY", "MC-200", "MC-100") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-1", "true")))
    }

    "should ignore resolved tickets" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val list = listOf(
                mockIssue(
                        key = "MC-100",
                        resolution = "Unresolved",
                        resolveAsDuplicate = {
                            resolvedList.add("MC-100")
                        },
                        createLink = {
                            type, key, outwards ->
                            linkMap["MC-100"] = listOf(type, key, outwards.toString())
                        }
                ),
                mockIssue(
                        key = "MC-200",
                        resolution = "Duplicate",
                        resolveAsDuplicate = {
                            resolvedList.add("MC-200")
                        },
                        createLink = {
                            type, key, outwards ->
                            linkMap["MC-200"] = listOf(type, key, outwards.toString())
                        }
                )
        )
        val issue = mockIssue(
                key = "MC-1",
                getOtherIssue = {
                    ticket -> list.first {
                    it.key == ticket
                }.right()
                }
        )

        command(issue, "ARISA_DUPLICATED_BY", "MC-200", "MC-100") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-1", "true")))
    }
})
