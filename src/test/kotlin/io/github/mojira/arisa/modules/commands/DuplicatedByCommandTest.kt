package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
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
            project = mockProject(
                key = "MC"
            ),
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
                project = mockProject(
                        key = "MC"
                ),
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

    "keys should be case-insensitive" {
        val command = DuplicatedByCommand()
        var resolvedVar = ""
        val linkList = mutableListOf<String>()
        val list = listOf(
                mockIssue(
                        key = "MC-100",
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
                project = mockProject(
                    key = "MC"
                ),
                key = "MC-1",
                getOtherIssue = {
                    ticket -> list.first {
                    it.key == ticket
                }.right()
                }
        )

        command(issue, "ARISA_DUPLICATED_BY", "mc-100") shouldBeRight ModuleResponse
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
                project = mockProject(
                    key = "MC"
                ),
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

    "should ignore when ticket key the same as the issue key" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val list = listOf(
                mockIssue(
                        key = "MC-100",
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
                project = mockProject(
                        key = "MC"
                ),
                key = "MC-200",
                getOtherIssue = {
                    ticket -> list.first {
                    it.key == ticket
                }.right()
                }
        )

        command(issue, "ARISA_DUPLICATED_BY", "MC-200", "MC-100") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-100"))
        linkMap shouldBe(mutableMapOf("MC-100" to listOf("Duplicate", "MC-200", "true")))
    }

    "should ignore unreachable/nonexistent keys" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val throwable = Throwable("fail")
        val child = mockIssue(
                key = "MC-100",
                resolveAsDuplicate = {
                    resolvedList.add("MC-100")
                },
                createLink = {
                    type, key, outwards ->
                    linkMap["MC-100"] = listOf(type, key, outwards.toString())
                }
        )
        val issue = mockIssue(
                project = mockProject(
                    key = "MC"
                ),
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
                project = mockProject(
                        key = "MC"
                ),
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

    "should resolve and link cross-project only when it is allowed by allowedCrossProjectDuplicates" {
        val command = DuplicatedByCommand()
        val resolvedList = mutableListOf<String>()
        val linkMap = mutableMapOf<String, List<String>>()
        val list = mutableListOf<Issue>()
        list.addAll(
            listOf(
                mockIssue(
                    project = mockProject(
                        key = "MCTEST"
                    ),
                    key = "MCTEST-1",
                    resolveAsDuplicate = {
                        resolvedList.add("MCTEST-1")
                    },
                    createLink = {
                        type, key, outwards ->
                        linkMap["MCTEST-1"] = listOf(type, key, outwards.toString())
                    },
                    getOtherIssue = { ticket: String -> list.first {
                        it.key == ticket
                    }.right() }
                ),
                mockIssue(
                    project = mockProject(
                        key = "MC"
                    ),
                    key = "MC-1",
                    resolveAsDuplicate = {
                        resolvedList.add("MC-1")
                    },
                    createLink = {
                        type, key, outwards ->
                        linkMap["MC-1"] = listOf(type, key, outwards.toString())
                    },
                    getOtherIssue = { ticket: String -> list.first {
                        it.key == ticket
                    }.right() }
                ),
                mockIssue(
                    project = mockProject(
                        key = "MCPE"
                    ),
                    key = "MCPE-1",
                    resolveAsDuplicate = {
                        resolvedList.add("MCPE-1")
                    },
                    createLink = {
                        type, key, outwards ->
                        linkMap["MCPE-1"] = listOf(type, key, outwards.toString())
                    },
                    getOtherIssue = { ticket: String -> list.first {
                        it.key == ticket
                    }.right() }
                ),
                mockIssue(
                    project = mockProject(
                        key = "REALMS"
                    ),
                    key = "REALMS-1",
                    resolveAsDuplicate = {
                        resolvedList.add("REALMS-1")
                    },
                    createLink = {
                        type, key, outwards ->
                        linkMap["REALMS-1"] = listOf(type, key, outwards.toString())
                    },
                    getOtherIssue = { ticket: String -> list.first {
                        it.key == ticket
                    }.right() }
                ),
                mockIssue(
                    project = mockProject(
                        key = "MCL"
                    ),
                    key = "MCL-1",
                    resolveAsDuplicate = {
                        resolvedList.add("MCL-1")
                    },
                    createLink = {
                        type, key, outwards ->
                        linkMap["MCL-1"] = listOf(type, key, outwards.toString())
                    },
                    getOtherIssue = { ticket: String -> list.first {
                        it.key == ticket
                    }.right() }
                ),
                mockIssue(
                    project = mockProject(
                        key = "BDS"
                    ),
                    key = "BDS-1",
                    resolveAsDuplicate = {
                        resolvedList.add("BDS-1")
                    },
                    createLink = {
                        type, key, outwards ->
                        linkMap["BDS-1"] = listOf(type, key, outwards.toString())
                    },
                    getOtherIssue = { ticket: String -> list.first {
                        it.key == ticket
                    }.right() }
                )
            )
        )

        command(list.first{ it.project.key == "MCTEST" }, "ARISA_DUPLICATED_BY", "MC-1", "MCPE-1", "REALMS-1", "MCL-1", "BDS-1") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf())
        linkMap shouldBe(mutableMapOf())
        resolvedList.clear()
        linkMap.clear()

        command(list.first{ it.project.key == "MC" }, "ARISA_DUPLICATED_BY", "MCTEST-1", "MCPE-1", "REALMS-1", "MCL-1", "BDS-1") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("REALMS-1", "MCL-1"))
        linkMap shouldBe(mutableMapOf("REALMS-1" to listOf("Duplicate", "MC-1", "true"), "MCL-1" to listOf("Duplicate", "MC-1", "true")))
        resolvedList.clear()
        linkMap.clear()

        command(list.first{ it.project.key == "MCPE" }, "ARISA_DUPLICATED_BY", "MC-1", "MCTEST-1", "REALMS-1", "MCL-1", "BDS-1") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("REALMS-1", "BDS-1"))
        linkMap shouldBe(mutableMapOf("REALMS-1" to listOf("Duplicate", "MCPE-1", "true"), "BDS-1" to listOf("Duplicate", "MCPE-1", "true")))
        resolvedList.clear()
        linkMap.clear()

        command(list.first{ it.project.key == "REALMS" }, "ARISA_DUPLICATED_BY", "MC-1", "MCPE-1", "MCTEST-1", "MCL-1", "BDS-1") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-1", "MCPE-1"))
        linkMap shouldBe(mutableMapOf("MC-1" to listOf("Duplicate", "REALMS-1", "true"), "MCPE-1" to listOf("Duplicate", "REALMS-1", "true")))
        resolvedList.clear()
        linkMap.clear()

        command(list.first{ it.project.key == "MCL" }, "ARISA_DUPLICATED_BY", "MC-1", "MCPE-1", "REALMS-1", "MCTEST-1", "BDS-1") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MC-1"))
        linkMap shouldBe(mutableMapOf("MC-1" to listOf("Duplicate", "MCL-1", "true")))
        resolvedList.clear()
        linkMap.clear()

        command(list.first{ it.project.key == "BDS" }, "ARISA_DUPLICATED_BY", "MC-1", "MCPE-1", "REALMS-1", "MCL-1", "MCTEST-1") shouldBeRight ModuleResponse
        resolvedList shouldBe(mutableListOf("MCPE-1"))
        linkMap shouldBe(mutableMapOf("MCPE-1" to listOf("Duplicate", "BDS-1", "true")))
    }
})
