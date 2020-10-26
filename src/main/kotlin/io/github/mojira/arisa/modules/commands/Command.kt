package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.domain.Issue

interface Command<A> {
    operator fun invoke(issue: Issue, arg: A): Int
}

interface Command2<A, B> {
    operator fun invoke(issue: Issue, arg1: A, arg2: B): Int
}
