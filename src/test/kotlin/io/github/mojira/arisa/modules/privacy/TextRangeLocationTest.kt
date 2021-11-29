package io.github.mojira.arisa.modules.privacy

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TextRangeLocationTest : FunSpec({
    data class TestData(
        val description: String,
        val textRangeLocation: TextRangeLocation,
        val expectedLocationString: String
    )

    listOf(
        TestData(
            "in first line",
            TextRangeLocation("first\nsecond", 2, 3),
            "2 - 3 (1:2 - 1:3)"
        ),
        TestData(
            "in middle line",
            TextRangeLocation("first\nsecond\nthird", 9, 10),
            "9 - 10 (2:3 - 2:4)"
        ),
        TestData(
            "in last line",
            TextRangeLocation("first\nsecond", 9, 10),
            "9 - 10 (2:3 - 2:4)"
        ),
        TestData(
            "spanning multiple lines",
            TextRangeLocation("first\nsecond", 2, 9),
            "2 - 9 (1:2 - 2:3)"
        ),
        TestData(
            "with CR",
            TextRangeLocation("first\rsecond", 9, 10),
            "9 - 10 (2:3 - 2:4)"
        ),
        TestData(
            "with CR LF",
            TextRangeLocation("first\r\nsecond", 9, 10),
            "9 - 10 (2:2 - 2:3)"
        ),
        TestData(
            "with mixed CR and LF",
            TextRangeLocation("first\nsecond\rthird\r\nfourth", 23, 24),
            "23 - 24 (4:3 - 4:4)"
        )
    ).forEach {
        test(it.description) {
            it.textRangeLocation.getLocationDescription() shouldBe it.expectedLocationString
        }
    }
})
