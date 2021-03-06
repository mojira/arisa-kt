package io.github.mojira.arisa.domain.service

interface UserService {
    fun getGroups(username: String): List<String>
}