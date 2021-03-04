package io.github.mojira.arisa.domain.service

import io.github.mojira.arisa.domain.new.Issue

interface IssueService {
    fun getIssue(key: String): Issue
    fun saveIssue(issue: Issue)
}