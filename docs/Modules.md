# Modules
Modules of Arisa. Arisa periodically checks for newly created Jira issues and updates to existing issues and then
invokes modules for these issues.

When a module is invoked it checks whether any action is needed for an issue, its comments or attachments. Each module
performs one specific task. The execution of a module can be customized through the
[config file](../config/config.yml)

## AffectedVersionMessage
| Entry | Value                                                                                     |
| ----- | ----------------------------------------------------------------------------------------- |
| Name  | `AffectedVersionMessage`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/AffectedVersionMessageModule.kt) |

Adds a message comment when an issue has a specific version as affected version. However, unlike other modules this
module does not perform any other action, it neither removes the version nor resolves the issue. This module is
intended for versions which are often erroneously added by users.

The map from Jira version ID to message key is specified as `versionIdMessageMap` in the [config](../config/config.yml)
(defaults to empty map).

### Checks
- The issue has been created after the last run.
- The issue has not been created by a staff member (helper, moderator or Mojang employee).

## Attachment
| Entry | Value                                                                         |
| ----- | ----------------------------------------------------------------------------- |
| Name  | `Attachment`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/AttachmentModule.kt) |

Removes attachments ending with certain extensions and adds a comment to inform the reporter.

### Checks
- There are attachments ending with any of the extensions
  listed in `extensionBlacklist` in the [config](../config/config.yml).

## CHK
| Entry | Value                                                                  |
| ----- | ---------------------------------------------------------------------- |
| Name  | `CHK`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/CHKModule.kt) |

Updates the `CHK` field with the current time when a ticket is firstly confirmed.

### Checks
- The `Confirmation Status` field is none of `null`, `"undefined"`, or `"unconfirmed"`, case-insensitively.
- The `CHK` field is `null`.

## Command
| Entry | Value                                                                      |
| ----- | -------------------------------------------------------------------------- |
| Name  | `Command`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/CommandModule.kt) |

Executes [commands](Commands.md).

### Checks
- There are comments which are
    - Restricted to `helper` or `staff`;
    - Sent by `helper`, `global-moderators`, or `staff`; AND
    - Starts with `ARISA_` and contains whitespaces.
- The comments will be interpreted according to the defined [command](Commands.md) syntax.

## ConfirmParent
| Entry | Value                                                                            |
| ----- | -------------------------------------------------------------------------------- |
| Name  | `ConfirmParent`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/ConfirmParentModule.kt) |

Sets `Confirmation Status` to `Community Consensus` when the amount of duplicated tickets is greater than the threshold.

### Checks
- The `Confirmation Status` field is listed in the `confirmationStatusWhitelist` defined in the [config](../config/config.yml).
- The amount of duplicated tickets that have unique reporters is greater than the `linkedThreshold` defined in the [config](../config/config.yml).

## Crash
| Entry | Value                                                                    |
| ----- | ------------------------------------------------------------------------ |
| Name  | `Crash`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/CrashModule.kt) |

Detects the crash reports of the ticket and resolves it as Duplicate/Invalid accordingly.

### Checks
- The `Confirmation Status` is `Unconfirmed`.
- The `Mojang Priority` is `null`.
- There are newly uploaded crash reports.
    - The extensions of the crash report can be defined in `crashExtensions` in the [config](../config/config.yml).
- All of the crash reports are either modded or duplicates of other tickets.
    - A list of the parents can be defined in `minecraftCrashDuplicates` and `jvmCrashDuplicates` in the
      [config](../config/config.yml).

## DuplicateMessage
| Entry | Value                                                                               |
| ----- | ----------------------------------------------------------------------------------- |
| Name  | `DuplicateMessage`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/DuplicateMessageModule.kt) |

Adds comment to indicate that this ticket is a duplicate if no one mentioned it before.

### Triggered

This module is triggered after the tickets are updated for `commentDelayMinutes` defined in the [config](../config/config.yml)

### Checks
- A duplicate link was created in `commentDelayMinutes` defined in the [config](../config/config.yml)
  before last run.
- The ticket doesn't have a staff restricted comment posted by a volunteer containing `ARISA_NO_DUPLICATE_MESSAGE`, `ARISA_NO_DUPE_MSG`, or a combination of these abbreviations.
- None of the duplicate links that were added to this ticket have been mentioned in any public comments.
- The ticket currently has parents.
- Adds the comment corresponding to the parents' security levels/resolutions/keys according to all the [config](../config/config.yml).

## EmptyModule
| Entry | Value                                                                    |
| ----- | ------------------------------------------------------------------------ |
| Name  | `EmptyModule`                                                            |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/EmptyModule.kt) |

Resolves an empty report as `Incomplete`.

### Checks
- The ticket was created after last run.
- The ticket is empty by either
    - Both description and environment are the built-in placeholder texts provided by Mojira; OR
    - Both description and environment are shorter than 5 characters.
- The ticket doesn't have any attachments.

## FutureVersion
| Entry | Value                                                                            |
| ----- | -------------------------------------------------------------------------------- |
| Name  | `FutureVersion`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/FutureVersionModule.kt) |

Removes future versions from the ticket, and resolves it as `Awaiting Response` when the future version is the only version
the ticket has.

### Checks
- Get all the versions of the ticket that are added after last run by non-`staff` users.
- Any of those versions is a future version.
- The project of this ticket has a released version.
- Adds the latest released version to `Affected Versions` and removes all the future versions. If the future version
  was the only version the ticket has, resolves the ticket as `Awaiting Response`.

## HideImpostors
| Entry | Value                                                                            |
| ----- | -------------------------------------------------------------------------------- |
| Name  | `HideImpostors`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/HideImpostorsModule.kt) |

Hides the comments created by users who look like an impostor.

### Checks
- The comment was created within a day.
- The comment's author's display name starts with a pair of square brackets with some texts in it, and has a space
  and some texts after the brackets. e.g. `[dev] foo`.
- The comment is not restricted to `staff`.
- The comment's author is not a `helper`, `global-moderators`, nor `staff`.

## KeepPlatform
| Entry | Value                                                                           |
| ----- | ------------------------------------------------------------------------------- |
| Name  | `KeepPlatform`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/KeepPlatformModule.kt) |

Keep tickets containing certain tag with the same `Platform`.

### Checks
- Change log contains `Platform` changes.
- Any of the comments is restricted to `staff` and contains the tag, as defined in the [config](../config/config.yml).
- Either there was a change by a user who is a `helper`, `global-moderators`, or `staff` after the comment, or there was any change after the comment (to get the saved platform)
- saved platform and current `Platform` do not match

## KeepPrivate
| Entry | Value                                                                          |
| ----- | ------------------------------------------------------------------------------ |
| Name  | `KeepPrivate`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/KeepPrivateModule.kt) |

Keep tickets containing certain tag as private.

### Checks
- The `tag` is defined in the [config](../config/config.yml).
- Any of the comments is restricted to `staff` and contains the tag.
- The security level of the ticket is not set to private.

## Language
| Entry | Value                                                                       |
| ----- | --------------------------------------------------------------------------- |
| Name  | `Language`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/LanguageModule.kt) |

Resolves tickets that are not in English as `Invalid`.

### Checks
- The ticket was created after last run.
- The ticket is viewable by public.
- Combines the summary and the description of the ticket together, separated with a space (` `).
  An dot (`.`) will be appended at the end of the summary and the description if there isn't any.
  Also if the summary is included in the description, only the description will be detected, vice versa.
- The length of the combined text exceeds the `lengthThreshold` defined in the [config](../config/config.yml).
- The detected language is not listed in the `allowedLanguages` defined in the [config](../config/config.yml)
  with a percentage greater than `0.7`.

## MultiplePlatforms
| Entry | Value                                                                                |
| ----- | ------------------------------------------------------------------------------------ |
| Name  | `MultiplePlatforms`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/MultiplePlatformsModule.kt) |

Sets `Platform` to `targetPlatform` (as defined in the [config](../config/config.yml)) when there is a duplicate link that leads to a ticket with different `Platform` to the one on the current issue.

### Checks
- Current `Platform` is in the whitelist, as defined by `platformWhitelist` in the [config](../config/config.yml).
- There's an inwards `Duplicate` link that leads to a ticket with a different platform to the current `platform`.
- The platform from the link is not blacklisted as defined by `transferredPlatformBlacklist` in the [config](../config/config.yml).
- There's no tag that shows that the ticket is supposed to keep its platform, as defined by `keepPlatformTag` in the [config](../config/config.yml).

## Piracy
| Entry | Value                                                                     |
| ----- | ------------------------------------------------------------------------- |
| Name  | `Piracy`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/PiracyModule.kt) |

Resolves tickets about pirated games as `Invalid`.

### Checks
- The ticket was created after last run.
- Any of the description, environment, and/or summary contains any of the `piracySignatures` defined in the [config](../config/config.yml).

## Privacy
| Entry | Value                                                                      |
| ----- | -------------------------------------------------------------------------- |
| Name  | `Privacy`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/PrivacyModule.kt) |

Hides privacy information like Email addresses in tickets or comments.

### Checks
- The ticket is not set to private.
#### For Setting Tickets to Private
- Any of the fields and/or text attachments added after last run contains session ID or Email.
- Or any of the attachments has a name specified by `sensitiveFileNames` in the [config](../config/config.yml)
  (defaults to empty list)
#### For Restricting Comments to `staff`
- The comment was added after last run.
- The comment is not restricted.
- The comment contains session ID or Email.

## PrivateDuplicate
| Entry | Value                                                                               |
| ----- | ----------------------------------------------------------------------------------- |
| Name  | `PrivateDuplicate`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/PrivateDuplicateModule.kt) |

Transfers security level and MEQS comment to a report's duplicates.

### Checks
- The `keepPrivateTag` is defined in the [config](../config/config.yml).
- The ticket has links.
- At least one link is outwards and duplicate.
- The ticket is not set to private.
- The linked report is private.
#### For Transferring MEQS Comment
- Any of the comments on the linked report is restricted to `staff` and contains the keepPrivateTag.

## RemoveBotComment
| Entry | Value                                                                                  |
|-------|----------------------------------------------------------------------------------------|
| Name  | `RemoveBotComment`                                                                     |
| Class | [link](../src/main/kotlin/io/github/mojira/arisa/modules/RemoveBotCommentModule.kt)    |

Deletes any undesired comments that were created by the bot that now only contain a removal tag (can be configured in [config](../config/config.yml)).

### Checks
- The comment was originally authored by the bot.
- The comment has been updated since the last run.
- The comment contains only the removal tag.
- The comment is restricted to `global-moderators` or `staff`.

## RemoveIdenticalLink
| Entry | Value                                                                                  |
| ----- | -------------------------------------------------------------------------------------- |
| Name  | `RemoveIdenticalLink`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/RemoveIdenticalLinkModule.kt) |

Removes identical links from the ticket.

### Checks
- The ticket has links.
- The link has the same type, direction, and linked ticket as any other link.
    - The direction of `Relates to` link isn't checked.

## RemoveNonStaffTags
| Entry | Value                                                                                 |
| ----- | ------------------------------------------------------------------------------------- |
| Name  | `RemoveNonStaffTags`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/RemoveNonStaffTagsModule.kt) |

Removes specified tags added by non-volunteer users.

### Checks
- The comment is a specified tag as defined in the config.
- The comment is not restricted to `global-moderators`, `staff` or `helper`.

## RemoveSpam
| Entry | Value                                                                         |
| ----- | ----------------------------------------------------------------------------- |
| Name  | `RemoveSpam`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/RemoveSpamModule.kt) |

Restricts comments that follow certain patterns (can be configured in [config](../config/config.yml)).

### Checks
- The comment
    - is new (created after the last bot run)
    - was not posted by someone in the `helper`, `staff`, or `global-moderators` group
    - is not restricted already

## RemoveTriagedMeqs
| Entry | Value                                                                                |
| ----- | ------------------------------------------------------------------------------------ |
| Name  | `RemoveTriagedMeqs`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/RemoveTriagedMeqsModule.kt) |

Replaces MEQS tags in already triaged tickets with `MEQS_ARISA_REMOVED`.

### Checks
- The ticket has been triaged by either
    - Has a `Mojang Priority`; OR
    - Has a `Triaged Time`.

## RemoveVersion
| Entry | Value                                                                            |
| ----- | -------------------------------------------------------------------------------- |
| Name  | `RemoveVersion`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/RemoveVersionModule.kt) |

Removes affected versions added to resolved tickets.

### Checks
- The ticket is resolved.
- Affected versions have been added by a normal user.

## ReopenAwaiting
| Entry | Value                                                                             |
| ----- | --------------------------------------------------------------------------------- |
| Name  | `ReopenAwaiting`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/ReopenAwaitingModule.kt) |

Reopens ticket that is resolved as `Awaiting Response` when the ticket is updated, or adds a comment when the ticket shouldn't
be reopened because of the `MEQS_KEEP_AR` tag or because the ticket is too old.

### Checks
- The ticket is resolved as `Awaiting Response`.
- The ticket was updated after it was created for two seconds.
- There are valid updates after the ticket was resolved. Valid updates are:
    - Comments that are posted by users, except for:
        - Comments from users with one of the roles in `blacklistedRoles` as defined in the [config](../config/config.yml)
        - Comments from new users (users that don't have any comments or non-trash bug reports that are older than 24 hours)
    - Changes that are done by the reporter.
- There is no `MEQS_KEEP_AR` tag.
- If there is a `ARISA_REOPEN_OP` tag, the bug report can only be reopened by the reporter.
- The ticket was resolved within the `softARDays` defined in the [config](../config/config.yml)
  OR the update is done by the reporter.
- If all the other checks pass but not the last two, adds a comment to indicate the user unless it has already been commented before.

## ReplaceText
| Entry | Value                                                                          |
| ----- | ------------------------------------------------------------------------------ |
| Name  | `ReplaceText`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/ReplaceTextModule.kt) |

Replaces ticket links with ticket keys.

### Checks
#### For Description
- The ticket was created after last run.
- The description is not `null`.
- The description has ticket URLs that don't have any query parameters nor display texts other than the ticket key.
#### For Comments
- The comment was created after last run.
- The comment has ticket URLs that don't have any query parameters nor display texts other than the ticket key.

## ResolveTrash
| Entry | Value                                                                           |
| ----- | ------------------------------------------------------------------------------- |
| Name  | `ResolveTrash`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/ResolveTrashModule.kt) |

Resolves trashed tickets as `Invalid`.

### Checks
- The ticket is in `TRASH` project.

## RevokeConfirmation
| Entry | Value                                                                                 |
| ----- | ------------------------------------------------------------------------------------- |
| Name  | `RevokeConfirmation`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/RevokeConfirmationModule.kt) |

Revokes changes to `Confirmation Status` done by non-volunteers.

### Checks
- The `Confirmation Status` is not the same as the last one changed by users with group
  `helper`, `global-moderators`, or `staff`.

## Thumbnail
| Entry | Value                                                                        |
| ----- | ---------------------------------------------------------------------------- |
| Name  | `Thumbnail`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/ThumbnailModule.kt) |

Edits embedded image references to large images in the issue description and comments to use a thumbnail reference
instead.

### Checks
- For issues: The issue has been created after the last run (edits after the last run are ignored)
- For comments: The comment has been added after the last run (edits after the last run are ignored)
- The image reference refers to an attached image (URL references are not supported)
- The image reference does not specify any display settings (e.g. custom size)
- The image is wider than `maxImageWidth` or taller than `maxImageHeight` specified in the [config](../config/config.yml)
- While reading the image not more than `maxImageReadBytes` as specified in the [config](../config/config.yml)
  (defaults to 5 KiB) are read. Note that most image formats include the dimension information in the first few bytes.
- At most `maxImagesCount` as specified in the [config](../config/config.yml) (defaults to 10) will be processed per
  issue description respectively per comment

## TransferLinks
| Entry | Value                                                                               |
| ----- | ----------------------------------------------------------------------------------- |
| Name  | `TransferLinks`                                                                     |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/TransferVersionsModule.kt) |

Transfers links from duplicated tickets to their parents.

### Checks
- The ticket was linked as a duplicate after last run.
#### For Removing Links from Children
- The link is not a `Duplicates` link.
#### For Adding Links to Parents
- The link is not a `Duplicates` link.
- The parent doesn't have this link.
- The link doesn't point to the parent itself.

## TransferVersions
| Entry | Value                                                                           |
| ----- | ------------------------------------------------------------------------------- |
| Name  | `TransferVersions`                                                              |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/UpdateLinkedModule.kt) |

Transfers versions from duplicated tickets to their parents.

### Checks
- The ticket was linked as a duplicate after last run.
- The Jira version ID is not listed in `notTransferredVersionIds` specified in the [config](../config/config.yml)
  (defaults to empty list, i.e. all versions are transferred).
- The version doesn't exist in the parent yet.

## UpdateLinked
| Entry | Value                                                                           |
| ----- | ------------------------------------------------------------------------------- |
| Name  | `UpdateLinked`                                                                  |
| Class | [Link](../src/main/kotlin/io/github/mojira/arisa/modules/UpdateLinkedModule.kt) |

Updates the `Linked` field to indicate the amount of duplicates of this ticket.

### Triggered

This module is triggered after the tickets are updated for `updateIntervalHours` defined in the [config](../config/config.yml).

### Checks
- The amount of duplicates calculated from the change log is not the same as the value of the `Linked` field.
- There are link changes after the `Linked` field was last updated.
- The first link change after the `Linked` field was last updated is not within `updateIntervalHours` defined in the [config](../config/config.yml).
