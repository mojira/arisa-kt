package io.github.mojira.arisa.infrastructure.apiclient.models

/**
 * Atlassian Document Format type alias.
 */
typealias ADF = Map<String, String>

/**
 *
 */
typealias v2Body = String

/**
 * BodyType that's used for API requests/responses.
 * Read more in [docs](https://developer.atlassian.com/cloud/jira/platform/rest/v3/intro/#version).
 *  - For REST API v2 this should be set to [v2Body] (Markdown String).
 *  - For REST API v3 this should be set to [ADF].
 */
typealias BodyType = v2Body
