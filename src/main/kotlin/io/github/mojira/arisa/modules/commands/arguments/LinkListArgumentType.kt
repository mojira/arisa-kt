package io.github.mojira.arisa.modules.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import io.github.mojira.arisa.modules.commands.CommandExceptions
import io.github.mojira.arisa.modules.concatLinkName
import io.github.mojira.arisa.modules.convertLinks
import io.github.mojira.arisa.modules.isTicketKey
import io.github.mojira.arisa.modules.splitElemsByCommas

data class LinkList(
    val type: String,
    val keys: List<String>
)

class LinkListArgumentType : ArgumentType<LinkList> {
    override fun parse(reader: StringReader): LinkList {
        val remaining = reader.remaining
        val list = remaining
            .split(" ")
            .toMutableList()
            .apply {
                splitElemsByCommas()
                concatLinkName()
            }

        val type = list[0]
        if (type == "") {
            throw CommandExceptions.INVALID_LINK_TYPE.createWithContext(reader)
        }

        list.apply {
            removeAt(0)
            convertLinks()
        }
        if (list.any { !it.isTicketKey() }) {
            throw CommandExceptions.INVALID_TICKET_KEY.createWithContext(reader)
        }

        reader.cursor = reader.totalLength

        return LinkList(type, list)
    }
}
