# API Token

Arisa requires an API token with scopes. The token can be generated 
in the [Profile Settings > Security > API Tokens](https://id.atlassian.com/manage-profile/security/api-tokens).

## Required Scopes

Classic scopes:
- `read:jira-work`
- `write:jira-work`
- `read:jira-user`

## Cloud ID

Additionally, a [Cloud ID](https://support.atlassian.com/jira/kb/retrieve-my-atlassian-sites-cloud-id/) is required to use the API token.
A quick and easy method is to visit:
```http request
https://<your-site-name>.atlassian.net/_edge/tenant_info
```
Save the ID into `local.yml`.

## Used API Endpoints Reference

### [GET /project/{projectIdOrKey}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-projects/#api-rest-api-2-project-projectidorkey-get)

Scopes (classic): `read:jira-work`


### [GET /issue/{issueIdOrKey}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issues/#api-rest-api-2-issue-issueidorkey-get)

Scopes (classic): `read:jira-work`


### [PUT /issue/{issueIdOrKey}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issues/#api-rest-api-2-issue-issueidorkey-put)

Scopes (classic): `write:jira-work`


### [GET /myself](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-myself/#api-rest-api-2-myself-get)

Scopes (classic): `read:jira-user`


### [GET /user/groups](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-users/#api-rest-api-2-user-groups-get)

Scopes (classic): `read:jira-user`


### [POST /search](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-search/#api-rest-api-2-search-post)

Scopes (classic): `read:jira-work`


### [GET /attachment/content/{id}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-attachments/#api-rest-api-2-attachment-content-id-get)

Scopes (classic): `read:jira-work`


### [POST /issue/{issueIdOrKey}/attachments](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-attachments/#api-rest-api-2-issue-issueidorkey-attachments-post)

Scopes (classic): `write:jira-work`


### [DELETE /attachment/{id}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-attachments/#api-rest-api-2-attachment-id-delete)

Scopes (classic): `write:jira-work`


### [POST /issue/{issueIdOrKey}/comment](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-comments/#api-rest-api-2-issue-issueidorkey-comment-post)

Scopes (classic): `write:jira-work`


### [PUT /issue/{issueIdOrKey}/comment/{projectIdOrKey}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-comments/#api-rest-api-2-issue-issueidorkey-comment-id-put)

Scopes (classic): `write:jira-work`


### [DELETE /issue/{issueIdOrKey}/comment/{projectIdOrKey}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-comments/#api-rest-api-2-issue-issueidorkey-comment-id-delete)

Scopes (classic): `write:jira-work`


### [POST /issueLink](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-links/#api-rest-api-2-issuelink-post)

Scopes (classic): `write:jira-work`


### [GET /issueLink/{linkId}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-links/#api-rest-api-2-issuelink-linkid-get)

Scopes (classic): `read:jira-work`


### [DELETE /issueLink/{linkId}](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-links/#api-rest-api-2-issuelink-linkid-delete)

Scopes (classic): `write:jira-work`


### [GET /issue/{issueIdOrKey}/transitions](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issues/#api-rest-api-2-issue-issueidorkey-transitions-get)

Scopes (classic): `read:jira-work`


### [POST /issue/{issueIdOrKey}/transitions](https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issues/#api-rest-api-2-issue-issueidorkey-transitions-post)

Scopes (classic): `write:jira-work`