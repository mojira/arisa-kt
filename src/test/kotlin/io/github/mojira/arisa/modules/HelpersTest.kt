package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.time.Instant

private val throwable = Throwable(message = "generic error")
private const val success = "success"
private val NOW = Instant.now()
private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class HelpersTest : StringSpec({
    "toFailedModuleEither when given Throwable in Either should return FailedModuleResponse with that 1 throwable" {
        val moduleEither = throwable.left().toFailedModuleEither()

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(FailedModuleResponse(listOf(throwable)))
    }

    /* commented out as I don't know yet whether this is a feature or not
    "toFailedModuleEither when given success in Either should return the success in ModuleResponse" {
        val moduleEither = success.right().toFailedModuleEither()

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(success)
    }
    */

    "toFailedModuleEither when given list with Throwables in Either should return FailedModuleResponse with all the Throwables in Either" {
        val moduleEither = listOf(success.right(), Throwable(message = "generic error 1").left(), success.right(),
            success.right(),  Throwable(message = "generic error 2").left(),
            Throwable(message = "generic error 3").left()).toFailedModuleEither()

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBeTypeOf<FailedModuleResponse>()
        (moduleEither.a as FailedModuleResponse).exceptions.shouldBe(listOf(Throwable(message = "generic error 1"),
            Throwable(message = "generic error 2"), Throwable(message = "generic error 3")))
    }

    "toFailedModuleEither when given list without Throwables in Either should return ModuleResponse in Either" {
        val moduleEither = listOf(success.right(), success.right(), success.right()).toFailedModuleEither()

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "Either.invert when given ModuleResponse in Either should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = ModuleResponse.right().invert()

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "Either.invert when given OperationNotNeededModuleResponse in Either should return ModuleResponse in Either" {
        val moduleEither = OperationNotNeededModuleResponse.left().invert()

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertContains when given String that contains the other String should return ModuleResponse in Either" {
        val moduleEither = assertContains("testabctest", "abc")

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertContains when given null should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertContains(null, "abc")

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertContains when given empty String should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertContains("", "abc")

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertContains when given String that does not contain other strings should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertContains("testabtestab", "abc")

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertEmpty when given empty list should return ModuleResponse in Either" {
        val moduleEither = assertEmpty(listOf<Int>())

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertEmpty when given nonempty list should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertEmpty(listOf<Int>(1337))

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertNotEmpty when given nonempty list should return ModuleResponse in Either" {
        val moduleEither = assertNotEmpty(listOf<Int>(1337))

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertNotEmpty when given empty list should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNotEmpty(listOf<Int>())

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertNull when given null should return ModuleResponse in Either" {
        val moduleEither = assertNull(null)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertNull when given not null should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNull(1337)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertNotNull when given not null should return ModuleResponse in Either" {
        val moduleEither = assertNotNull(1337)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertNotNull when given null should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNotNull(null)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertEither when given arguments one of which returns ModuleResponse in Either it should return ModuleResponse in Either" {
        val moduleEither = assertEither(OperationNotNeededModuleResponse.left(), ModuleResponse.right())

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertEither when given arguments none of which return ModuleResponse in Either it should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertEither(OperationNotNeededModuleResponse.left(), OperationNotNeededModuleResponse.left())

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
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
        val moduleEither = tryRunAll(listOf({successful1()}, {successful2()}, {successful3()}))

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
        checker.shouldBe("abc")
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
        val moduleEither = tryRunAll(listOf({successful1()}, {successful2()}, {throwable.left()}, {successful3()}))

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(FailedModuleResponse(listOf(throwable)))
        checker.shouldBe("ab")
    }

    "assertEquals when given equal arguments should return ModuleResponse in Either" {
        val moduleEither = assertEquals(1337, 1337)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertEquals when given not equal arguments should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertEquals(1337, 7331)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertNotEquals when given not equal arguments should return ModuleResponse in Either" {
        val moduleEither = assertNotEquals(1337, 7331)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertNotEquals when given equal arguments should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertNotEquals(1337, 1337)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertTrue when given true should return ModuleResponse in Either" {
        val moduleEither = assertTrue(true)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertTrue when given false should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertTrue(false)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertFalse when given false should return ModuleResponse in Either" {
        val moduleEither = assertFalse(false)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertFalse when given true should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertFalse(true)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertGreaterThan when given 2 arguments first of which is greater than the second should return ModuleResponse in Either" {
        val moduleEither = assertGreaterThan(2, 1)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertGreaterThan when given 2 arguments first of which is smaller than the second should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertGreaterThan(1, 2)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertGreaterThan when given 2 arguments that are equal to each other should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertGreaterThan(1, 1)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertAfter when given 2 times first of which is after the second should return ModuleResponse in Either" {
        val moduleEither = assertAfter(NOW, A_SECOND_AGO)

        moduleEither.shouldBeRight()
        (moduleEither as Either.Right).b.shouldBe(ModuleResponse)
    }

    "assertAfter when given 2 times first of which is before the second should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertAfter(A_SECOND_AGO, NOW)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }

    "assertAfter when given 2 times that are equal to each other should return OperationNotNeededModuleResponse in Either" {
        val moduleEither = assertAfter(A_SECOND_AGO, A_SECOND_AGO)

        moduleEither.shouldBeLeft()
        (moduleEither as Either.Left).a.shouldBe(OperationNotNeededModuleResponse)
    }
})