package io.github.mojira.arisa.infrastructure

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JqlUtilsTest : StringSpec({
    val template = { it: String -> "query $it user" }

    "should properly quote regular user names" {
        escapeIssueFunction("username", template) shouldBe """ "query 'username' user" """.trim()
    }

    "should properly quote user names with a double quote" {
        escapeIssueFunction("user\"name", template) shouldBe """ 'query \'user"name\' user' """.trim()
    }

    "should properly quote user names with a single quote" {
        escapeIssueFunction("user'name", template) shouldBe """ "query \"user'name\" user" """.trim()
    }
})
