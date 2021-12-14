# Commands
Commands supported by Arisa. Commands are executed by adding a restricted comment to a Jira issue with each line
containing a command. After commands have been executed, Arisa will edit the original comment to include the command
results.

Most commands are executed for the issue on which the comment was added, however there are also some commands which
are executed globally.

## $ARISA_ADD_LINKS
| Entry       | Value                                            |
| ----------- | ------------------------------------------------ |
| Syntax      | `$ARISA_ADD_LINKS <link type> <list of tickets>` |
| Permissions | Mod+                                             |

Examples of types: "duplicated by", "is duplicated", "is duplicated by", "duplicated"

List of tickets can contain commas

List of tickets can consist of keys (MC-1) or links to tickets (https://bugs.mojang.com/browse/MC-1)

Keys and type are case insensitive

## $ARISA_ADD_VERSION
| Entry       | Value                          |
| ----------- | ------------------------------ |
| Syntax      | `$ARISA_ADD_VERSION <version>` |
| Permissions | Helper+                        |

- `<version>`: Put the name of the version on Mojira as-is, including potential spaces. Do not put quotes around it.

## $ARISA_CLEAR_PROJECT_CACHE
| Entry       | Value                          |
| ----------- | ------------------------------ |
| Syntax      | `$ARISA_CLEAR_PROJECT_CACHE`   |
| Permissions | Helper+                        |

Clears the project cache.

Arisa has a project cache that only gets updated every 5 minutes.

This means that if a new version is released, Arisa might only notice this after a certain amount of time.

In oder to avoid complications (e.g. with the FutureVersion module), any helper or mod can run this command
on any bug report in order to force Arisa to clear its project cache.

This command currently only exists because of a technical limitation. It will be removed in the future.

## $ARISA_DEOBFUSCATE
| Entry       | Value                                                                       |
| ----------- | --------------------------------------------------------------------------- |
| Syntax      | `$ARISA_DEOBFUSCATE <attachmentId> [<minecraftVersionId> [client\|server]]` |
| Permissions | Helper+                                                                     |

Deobfuscates a crash report attachment with the specified Jira attachment ID and attaches the deobfuscated crash report.
The attachment ID can be found in the URL of the attachment.

`<minecraftVersionId>` and crash report type (`client` or `server`) can be used to overwrite the values automatically
detected by Arisa. They must be specified when Arisa is unable to detect them automatically.

Note: The 'Crash' module automatically attaches deobfuscated crash reports. This command exists for cases where Arisa
was unable to parse the crash report or detected the wrong Minecraft version.

## $ARISA_FIX_CAPITALIZATION
| Entry       | Value                       |
| ----------- | --------------------------- |
| Syntax      | `$ARISA_FIX_CAPITALIZATION` |
| Permissions | Helper+                     |

Lowercases sentences in which the first letter of each word is capitalized.

## $ARISA_FIXED
| Entry       | Value                            |
| ----------- | -------------------------------- |
| Syntax      | `$ARISA_FIXED <version> [force]` |
| Permissions | Mod+                             |

- `<version>`: Put the name of the version on Mojira as-is, including potential spaces. Do not put quotes around it.

Resolves an issue as Fixed in the specified version. This command is useful when the version has already been
archived, and the web interface does not allow choosing that version.

When one of the affected versions of the issue was erroneously added and the fixed version is the same or an earlier
version, the command fails. To skip this check, run with a trailing `force` literal, e.g. `$ARISA_FIXED 12w34a force`.

## $ARISA_LIST_USER_ACTIVITY
| Entry       | Value                                  |
| ----------- | -------------------------------------- |
| Syntax      | `$ARISA_LIST_USER_ACTIVITY <username>` |
| Permissions | Mod+                                   |

- `<username>`: Copy from the Username field on the user's profile as-is (not from the URL, where spaces may get encoded as plus signs). Do not put quotes around it. Note that there might be a trailing space in the Username field -- you can leave it in the command as Arisa doesn't mind it.

List all recent activity (up to 50 items) by the given user in a new mod+ comment.

## $ARISA_MAKE_PRIVATE
| Entry       | Value                       |
| ----------- | --------------------------- |
| Syntax      | `$ARISA_MAKE_PRIVATE`       |
| Permissions | Helper+                     |

Causes Arisa to make the ticket private. This command allows helpers to quickly make a sensitive ticket private
without having to wait for a moderator. Moderators don't need to use this command, they can directly change
the Security Level.

This command only has an effect once; the reporter is able to make the ticket public afterwards again.

## $ARISA_PURGE_ATTACHMENT
| Entry       | Value                                                      |
| ----------- | ---------------------------------------------------------- |
| Syntax      | `$ARISA_PURGE_ATTACHMENT <username> [<min ID>] [<max ID>]` |
| Permissions | Mod+                                                       |

- `<username>`: Copy from the Username field on the user's profile as-is (not from the URL, where spaces may get encoded as plus signs). Do not put quotes around it. Note that there might be a trailing space in the Username field -- you can leave it in the command as Arisa doesn't mind it.

Deletes all attachments on the ticket by user with the username if the attachment ID is between min ID and max ID.

Without specifying min/max ID, all attachments by that user are deleted.

Note: the numeric ID of an attachment can be found in the change log or in the link of it.

## $ARISA_REMOVE_COMMENTS
| Entry       | Value                               |
| ----------- | ----------------------------------- |
| Syntax      | `$ARISA_REMOVE_COMMENTS <username>` |
| Permissions | Mod+                                |

- `<username>`: Copy from the Username field on the user's profile as-is (not from the URL, where spaces may get encoded as plus signs). Do not put quotes around it. Note that there might be a trailing space in the Username field -- you can leave it in the command as Arisa doesn't mind it.

Finds all comments created by the user on the current report and restricts them.

Note: the command is successful regardless of whether any comments by that user exist.

## $ARISA_REMOVE_CONTENT
| Entry       | Value                              |
| ----------- | ---------------------------------- |
| Syntax      | `$ARISA_REMOVE_CONTENT <username>` |
| Permissions | Mod+                               |

(Formerly `$ARISA_REMOVE_USER`)

- `<username>`: Copy from the Username field on the user's profile as-is (not from the URL, where spaces may get encoded as plus signs). Do not put quotes around it. Note that there might be a trailing space in the Username field -- you can leave it in the command as Arisa doesn't mind it.

Finds all comments and attachments created by the user on any report and restricts or deletes them, respectively.

Note: The command executes successfully regardless of whether any comments by that user exist. The bot will report back
with a new comment when it has finished removing the user.

## $ARISA_REMOVE_LINKS
| Entry       | Value                                               |
| ----------- | --------------------------------------------------- |
| Syntax      | `$ARISA_REMOVE_LINKS <link type> <list of tickets>` |
| Permissions | Mod+                                                |

Examples of types: "duplicated by", "is duplicated", "is duplicated by", "duplicated"

List of tickets can contain commas

List of tickets can consist of keys (MC-1) or links to tickets (https://bugs.mojang.com/browse/MC-1)

Keys and type are case insensitive

## $ARISA_REOPEN
| Entry       | Value           |
| ----------- | --------------- |
| Syntax      | `$ARISA_REOPEN` |
| Permissions | Helper+         |

Reopens tickets resolved as Awaiting Response

## $ARISA_SHADOWBAN
| Entry       | Value                         |
| ----------- | ----------------------------- |
| Syntax      | `$ARISA_SHADOWBAN <username>` |
| Permissions | Mod+                          |

- `<username>`: Copy from the Username field on the user's profile as-is (not from the URL, where spaces may get encoded as plus signs). Do not put quotes around it. Note that there might be a trailing space in the Username field -- you can leave it in the command as Arisa doesn't mind it.

Shadowbans a user for the next 24 hours.

Note: The user's previous actions still need to be reverted manually (or e.g. via `$ARISA_REMOVE_CONTENT` and bulk change),
as running this command will only cause the bot auto-undo actions done by the shadowbanned user in the future.

For more information on how shadowbans work, see "Modules → Shadowban".
