package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private val throwable = Throwable(message = "generic error")
private const val success = "success"
private val NOW = Instant.now()
private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class HelpersTest : StringSpec({
    "toFailedModuleEither when given Throwable in Either should return FailedModuleResponse with that 1 throwable" {
        val moduleEither = throwable.left().toFailedModuleEither()

        moduleEither shouldBeLeft FailedModuleResponse(listOf(throwable))
    }

    "toFailedModuleEither when given success in Either should return the success in ModuleResponse" {
        val moduleEither = success.right().toFailedModuleEither()

        moduleEither shouldBeRight success
    }

    "toFailedModuleEither when given list with Throwables in Either should return FailedModuleResponse with all the Throwables in Either" {
        val throwable1 = Throwable(message = "generic error 1")
        val throwable2 = Throwable(message = "generic error 2")
        val throwable3 = Throwable(message = "generic error 3")
        val moduleEither = listOf(success.right(), throwable1.left(), success.right(), success.right(),
            throwable2.left(), throwable3.left()).toFailedModuleEither()

        moduleEither shouldBeLeft FailedModuleResponse(listOf(throwable1, throwable2, throwable3))
    }

    "toFailedModuleEither when given list without Throwables in Either should return ModuleResponse in Either" {
        val moduleEither = listOf(success.right(), success.right(), success.right()).toFailedModuleEither()

        moduleEither shouldBeRight ModuleResponse
    }

    "Either.invert when given ModuleResponse in Either should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = ModuleResponse.right().invert()

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "Either.invert when given OperationNotNeededModuleResponse in Either should return ModuleResponse in Either" {
        val moduleEither = OperationNotNeededModuleResponse.left().invert()

        moduleEither shouldBeRight ModuleResponse
    }

    "assertContains when given String that contains the other String should return ModuleResponse in Either" {
        val moduleEither = assertContains("testabctest", "abc")

        moduleEither shouldBeRight ModuleResponse
    }

    "assertContains when given null should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertContains(null, "abc")

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertContains when given empty String should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertContains("", "abc")

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertContains when given String that does not contain other strings should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertContains("testabtestab", "abc")

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertEmpty when given empty list should return ModuleResponse in Either" {
        val moduleEither = assertEmpty(listOf<Int>())

        moduleEither shouldBeRight ModuleResponse
    }

    "assertEmpty when given nonempty list should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertEmpty(listOf(1337))

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertNotEmpty when given nonempty list should return ModuleResponse in Either" {
        val moduleEither = assertNotEmpty(listOf(1337))

        moduleEither shouldBeRight ModuleResponse
    }

    "assertNotEmpty when given empty list should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNotEmpty(listOf<Int>())

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertNull when given null should return ModuleResponse in Either" {
        val moduleEither = assertNull(null)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertNull when given not null should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNull(1337)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertNotNull when given not null should return ModuleResponse in Either" {
        val moduleEither = assertNotNull(1337)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertNotNull when given null should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNotNull(null)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertEither when given arguments one of which returns ModuleResponse in Either it should return ModuleResponse in Either" {
        val moduleEither = assertEither(OperationNotNeededModuleResponse.left(), ModuleResponse.right())

        moduleEither shouldBeRight ModuleResponse
    }

    "assertEither when given arguments none of which return ModuleResponse in Either it should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertEither(OperationNotNeededModuleResponse.left(), OperationNotNeededModuleResponse.left())

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "tryRunAll when given functions that run successfully should run all of them and return ModuleResponse in Either" {
        var checker = ""
        fun successful1(): Either<Throwable, Unit> {
            checker += "a"
            return Unit.right()
        }
        fun successful2(): Either<Throwable, Unit> {
            checker += "b"
            return Unit.right()
        }
        fun successful3(): Either<Throwable, Unit> {
            checker += "c"
            return Unit.right()
        }
        val moduleEither = tryRunAll(listOf({ successful1() }, { successful2() }, { successful3() }))

        moduleEither shouldBeRight ModuleResponse
        checker shouldBe("abc")
    }

    "tryRunAll when given functions that run successfully and one that throws should run all of the ones before the Throwable and return FailedModuleResponse in Either" {
        var checker = ""
        fun successful1(): Either<Throwable, Unit> {
            checker += "a"
            return Unit.right()
        }
        fun successful2(): Either<Throwable, Unit> {
            checker += "b"
            return Unit.right()
        }
        fun successful3(): Either<Throwable, Unit> {
            checker += "c"
            return Unit.right()
        }
        val moduleEither = tryRunAll(listOf({ successful1() }, { successful2() }, { throwable.left() },
            { Throwable(message = "different error").left() }, { successful3() }))

        moduleEither shouldBeLeft FailedModuleResponse(listOf(throwable))
        checker shouldBe("ab")
    }

    "assertEquals when given equal arguments should return ModuleResponse in Either" {
        val moduleEither = assertEquals(1337, 1337)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertEquals when given not equal arguments should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertEquals(1337, 7331)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertNotEquals when given not equal arguments should return ModuleResponse in Either" {
        val moduleEither = assertNotEquals(1337, 7331)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertNotEquals when given equal arguments should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNotEquals(1337, 1337)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertTrue when given true should return ModuleResponse in Either" {
        val moduleEither = assertTrue(true)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertTrue when given false should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertTrue(false)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertFalse when given false should return ModuleResponse in Either" {
        val moduleEither = assertFalse(false)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertFalse when given true should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertFalse(true)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertGreaterThan when given 2 arguments first of which is greater than the second should return ModuleResponse in Either" {
        val moduleEither = assertGreaterThan(2, 1)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertGreaterThan when given 2 arguments first of which is smaller than the second should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertGreaterThan(1, 2)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertGreaterThan when given 2 arguments that are equal to each other should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertGreaterThan(1, 1)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertAfter when given 2 times first of which is after the second should return ModuleResponse in Either" {
        val moduleEither = assertAfter(NOW, A_SECOND_AGO)

        moduleEither shouldBeRight ModuleResponse
    }

    "assertAfter when given 2 times first of which is before the second should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertAfter(A_SECOND_AGO, NOW)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "assertAfter when given 2 times that are equal to each other should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertAfter(A_SECOND_AGO, A_SECOND_AGO)

        moduleEither shouldBeLeft OperationNotNeededModuleResponse
    }

    "splitArgsByCommas should split arguments by commas" {
        val list = mutableListOf("1", "2,", ",3", "4,5")
        list.splitElemsByCommas()

        list shouldBe(listOf("1", "2", "3", "4", "5"))
    }

    "splitArgsByCommas should not include empty strings in the result" {
        val list = mutableListOf("1", "2,,,", ",,,,,", ",,,,3", "4,,,,,5")
        list.splitElemsByCommas()

        list shouldBe(listOf("1", "2", "3", "4", "5"))
    }

    "checkIfLinkNameRegexMatches should match valid ticket key" {
        "MC-100".checkIfLinkNameRegexMatches() shouldBe(true)
    }

    "checkIfLinkNameRegexMatches should match valid ticket link" {
        "https://bugs.mojang.com/browse/MC-100".checkIfLinkNameRegexMatches() shouldBe(true)
    }

    "checkIfLinkNameRegexMatches should not match invalid values" {
        "".checkIfLinkNameRegexMatches() shouldBe(false)
        "MC-100a".checkIfLinkNameRegexMatches() shouldBe(false)
        "MC-a100".checkIfLinkNameRegexMatches() shouldBe(false)
        "MCa-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "aMC-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "MC1-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "1MC-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "MC-".checkIfLinkNameRegexMatches() shouldBe(false)
        "MC100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/MC-100a".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/MC-a100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/MCa-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/aMC-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/MC1-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/1MC-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/-100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/MC-".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://bugs.mojang.com/browse/MC100".checkIfLinkNameRegexMatches() shouldBe(false)
        "https://google.com/browse/MC-100".checkIfLinkNameRegexMatches() shouldBe(false)
    }

    "concatLinkName should concatenate the string out of the array until it reaches valid ticket number/link" {
        val list1 = mutableListOf("1", "2", "3", "MC-4", "MC-5", "MC-6")
        list1.concatLinkName()
        list1 shouldBe(mutableListOf("1 2 3", "MC-4", "MC-5", "MC-6"))

        val list2 = mutableListOf("1", "2", "3", "https://bugs.mojang.com/browse/MC-4",
                "https://bugs.mojang.com/browse/MC-5", "https://bugs.mojang.com/browse/MC-6")
        list2.concatLinkName()
        list2 shouldBe(mutableListOf("1 2 3", "https://bugs.mojang.com/browse/MC-4",
                "https://bugs.mojang.com/browse/MC-5", "https://bugs.mojang.com/browse/MC-6"))
    }

    "concatLinkName when given ticket number/link should add empty string to the beginning of the list, without changing the rest of the list" {
        val list1 = mutableListOf("MC-1", "MC-2", "MC-3", "MC-4", "MC-5")
        list1.concatLinkName()
        list1 shouldBe(mutableListOf("", "MC-1", "MC-2", "MC-3", "MC-4", "MC-5"))

        val list2 = mutableListOf("https://bugs.mojang.com/browse/MC-1", "https://bugs.mojang.com/browse/MC-2",
                "https://bugs.mojang.com/browse/MC-3", "https://bugs.mojang.com/browse/MC-4",
                "https://bugs.mojang.com/browse/MC-5")
        list2.concatLinkName()
        list2 shouldBe(mutableListOf("", "https://bugs.mojang.com/browse/MC-1", "https://bugs.mojang.com/browse/MC-2",
                "https://bugs.mojang.com/browse/MC-3", "https://bugs.mojang.com/browse/MC-4",
                "https://bugs.mojang.com/browse/MC-5"))
    }

    "concatLinkName when not given ticket number/link within first 4 elements should add empty string to the beginning of the list, without changing the rest of the list" {
        val list1 = mutableListOf("1", "2", "3", "4", "MC-5")
        list1.concatLinkName()
        list1 shouldBe(mutableListOf("", "1", "2", "3", "4", "MC-5"))

        val list2 = mutableListOf("1", "2", "3", "4", "https://bugs.mojang.com/browse/MC-5")
        list2.concatLinkName()
        list2 shouldBe(mutableListOf("", "1", "2", "3", "4", "https://bugs.mojang.com/browse/MC-5"))
    }

    "concatLinkName should not fail when there's 4 or less elements" {
        mutableListOf<String>().apply{ this.concatLinkName() }                       shouldBe(mutableListOf(""))

        mutableListOf("1", "2", "3", "4").apply{ this.concatLinkName() }             shouldBe(mutableListOf("", "1", "2", "3", "4"))
        mutableListOf("1", "2", "3").apply{ this.concatLinkName() }                  shouldBe(mutableListOf("", "1", "2", "3"))
        mutableListOf("1", "2").apply{ this.concatLinkName() }                       shouldBe(mutableListOf("", "1", "2"))
        mutableListOf("1").apply{ this.concatLinkName() }                            shouldBe(mutableListOf("", "1"))

        mutableListOf("MC-1", "MC-2", "MC-3", "MC-4").apply{ this.concatLinkName() } shouldBe(mutableListOf("", "MC-1", "MC-2", "MC-3", "MC-4"))
        mutableListOf("MC-1", "MC-2", "MC-3").apply{ this.concatLinkName() }         shouldBe(mutableListOf("", "MC-1", "MC-2", "MC-3"))
        mutableListOf("MC-1", "MC-2").apply{ this.concatLinkName() }                 shouldBe(mutableListOf("", "MC-1", "MC-2"))
        mutableListOf("MC-1").apply{ this.concatLinkName() }                         shouldBe(mutableListOf("", "MC-1"))

        mutableListOf("1", "2", "3", "MC-4").apply{ this.concatLinkName() }          shouldBe(mutableListOf("1 2 3", "MC-4"))
        mutableListOf("1", "2", "MC-3").apply{ this.concatLinkName() }               shouldBe(mutableListOf("1 2", "MC-3"))
        mutableListOf("1", "MC-2").apply{ this.concatLinkName() }                    shouldBe(mutableListOf("1", "MC-2"))

        mutableListOf("1", "2", "MC-3", "MC-4").apply{ this.concatLinkName() }       shouldBe(mutableListOf("1 2", "MC-3", "MC-4"))
        mutableListOf("1", "MC-2", "MC-3").apply{ this.concatLinkName() }            shouldBe(mutableListOf("1", "MC-2", "MC-3"))

        mutableListOf("1", "2", "MC-3", "4").apply{ this.concatLinkName() }          shouldBe(mutableListOf("1 2", "MC-3", "4"))
        mutableListOf("1", "MC-2", "3").apply{ this.concatLinkName() }               shouldBe(mutableListOf("1", "MC-2", "3"))
        mutableListOf("MC-1", "2").apply{ this.concatLinkName() }                    shouldBe(mutableListOf("", "MC-1", "2"))

        mutableListOf("1", "MC-2", "MC-3", "4").apply{ this.concatLinkName() }       shouldBe(mutableListOf("1", "MC-2", "MC-3", "4"))
        mutableListOf("MC-1", "MC-2", "3").apply{ this.concatLinkName() }            shouldBe(mutableListOf("", "MC-1", "MC-2", "3"))

        mutableListOf("1", "MC-2", "3", "MC-4").apply{ this.concatLinkName() }       shouldBe(mutableListOf("1", "MC-2", "3", "MC-4"))
        mutableListOf("MC-1", "2", "MC-3").apply{ this.concatLinkName() }            shouldBe(mutableListOf("", "MC-1", "2", "MC-3"))

        mutableListOf("1", "MC-2", "3", "4").apply{ this.concatLinkName() }          shouldBe(mutableListOf("1", "MC-2", "3", "4"))
        mutableListOf("MC-1", "2", "3").apply{ this.concatLinkName() }               shouldBe(mutableListOf("", "MC-1", "2", "3"))

        mutableListOf("1", "MC-2", "MC-3", "MC-4").apply{ this.concatLinkName() }    shouldBe(mutableListOf("1", "MC-2", "MC-3", "MC-4"))

        mutableListOf("MC-1", "MC-2", "MC-3", "4").apply{ this.concatLinkName() }    shouldBe(mutableListOf("", "MC-1", "MC-2", "MC-3", "4"))
        mutableListOf("MC-1", "MC-2", "3", "MC-4").apply{ this.concatLinkName() }    shouldBe(mutableListOf("", "MC-1", "MC-2", "3", "MC-4"))
        mutableListOf("MC-1", "2", "MC-3", "MC-4").apply{ this.concatLinkName() }    shouldBe(mutableListOf("", "MC-1", "2", "MC-3", "MC-4"))
        mutableListOf("MC-1", "2", "3", "MC-4").apply{ this.concatLinkName() }       shouldBe(mutableListOf("", "MC-1", "2", "3", "MC-4"))
        mutableListOf("MC-1", "2", "MC-3", "4").apply{ this.concatLinkName() }       shouldBe(mutableListOf("", "MC-1", "2", "MC-3", "4"))
        mutableListOf("MC-1", "MC-2", "3", "4").apply{ this.concatLinkName() }       shouldBe(mutableListOf("", "MC-1", "MC-2", "3", "4"))
        mutableListOf("MC-1", "2", "3", "4").apply{ this.concatLinkName() }          shouldBe(mutableListOf("", "MC-1", "2", "3", "4"))
    }

    "convertLinks should convert ticket links to numbers" {
        val list = mutableListOf("https://bugs.mojang.com/browse/MC-1", "https://bugs.mojang.com/browse/MC-20",
                "https://bugs.mojang.com/browse/MC-312", "https://bugs.mojang.com/browse/MC-400")
        list.convertLinks()
        list shouldBe(mutableListOf("MC-1", "MC-20", "MC-312", "MC-400"))
    }

    "convertLinks should not touch ticket numbers" {
        val list1 = mutableListOf("MC-1", "MC-20", "MC-312", "MC-400")
        list1.convertLinks()
        list1 shouldBe(mutableListOf("MC-1", "MC-20", "MC-312", "MC-400"))

        val list2 = mutableListOf("https://bugs.mojang.com/browse/MC-1", "MC-20",
                "https://bugs.mojang.com/browse/MC-312", "MC-400")
        list2.convertLinks()
        list2 shouldBe(mutableListOf("MC-1", "MC-20", "MC-312", "MC-400"))
    }

    "in created LinkType nameVariants should contain a sorted set of all possible combinations (without skipping middle item and without changing order) of concatenated elements of name list" {
        LinkType(listOf("1", "2", "3"), "123", true).nameVariants shouldBe(sortedSetOf("1", "1 2", "1 2 3", "2", "2 3", "3"))
    }
})
