package io.github.mojira.arisa.domain.service

import io.github.mojira.arisa.domain.Issue

interface IssueService {
    fun getIssue(key: String): Issue
    fun saveIssue(issue: Issue)
    fun exportIssue(issue: Issue)
    fun searchIssues(query: String): List<Issue>
    fun cleanup()
}