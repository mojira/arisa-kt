package io.github.mojira.arisa.domain.service

import io.github.mojira.arisa.domain.new.Issue

interface UserService {
    fun getGroups(username: String): List<String>
}