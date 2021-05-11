package io.github.mojira.arisa.infrastructure

fun escapeIssueFunction(username: String, template: (username: String) -> String): String {
    val escapedUserName = when {
        username.contains('\'') -> """\"$username\""""
        username.contains('"') -> """\'$username\'"""
        else -> "'$username'"
    }
    val delim = if (username.contains('"')) "'" else "\""

    return delim + template(escapedUserName) + delim
}
