package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.right
import io.github.mojira.arisa.credentials
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.jira.getIssue
import io.github.mojira.arisa.jiraClient
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.assertTrue
import org.apache.http.HttpHost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicHttpRequest
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

const val DIVISOR = 10
class RemoveUserCommand : Command {
    val regex = "\"https:\\/\\/bugs\\.mojang\\.com\\/browse\\/(.*)\">".toRegex()
    override fun invoke(issue: Issue, vararg arguments: String): Either<ModuleError, ModuleResponse> = Either.fx {
        assertTrue(arguments.size > 1).bind()
        val name = arguments.asList().subList(1, arguments.size).joinToString(" ")
        val streamName = name.replace("+", "_").replace("_", "%5C_")
        val request = BasicHttpRequest("GET", "/activity?maxResults=200&streams=user+IS+$streamName")
        credentials.authenticate(request)
        val inputStream = DefaultHttpClient().execute(HttpHost("bugs.mojang.com"), request).entity.content
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val entries = doc.getElementsByTagName("entry")

        val tickets = mutableListOf<String>()
        for (i in 0 until entries.length) {
            tickets.add(entries.item(i).childNodes.item(1).textContent.parseTitle())
        }

        Thread {
            tickets
                .toSet()
                .filterNot { it.startsWith("TRASH-") }
                .map {
                    val either = getIssue(jiraClient, it)
                    if (either.isLeft()) {
                        null
                    } else {
                        (either as Either.Right).b
                    }
                }
                .filterNotNull()
                .forEach {
                    it.comments
                        .filter { it.visibility?.type != "staff" }
                        .filter { it.author.name == name }
                        .forEachIndexed { index, it ->
                            it.update("Removed by Arisa - Delete user $name", "group", "staff")
                            if (index % DIVISOR == 0) {
                                TimeUnit.SECONDS.sleep(1)
                            }
                        }
                }
        }.start()
        ModuleResponse.right()
    }

    private fun String.parseTitle() = regex.find(this)!!.groupValues[1]
}
