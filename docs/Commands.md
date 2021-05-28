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

## $ARISA_FIX_CAPITALIZATION
| Entry       | Value                       |
| ----------- | --------------------------- |
| Syntax      | `$ARISA_FIX_CAPITALIZATION` |
| Permissions | Helper+                     |

Lowercases sentences in which the first letter of each word is capitalized.

## $ARISA_FIXED
| Entry       | Value                    |
| ----------- | ------------------------ |
| Syntax      | `$ARISA_FIXED <version>` |
| Permissions | Mod+                     |

## $ARISA_LIST_USER_ACTIVITY
| Entry       | Value                                  |
| ----------- | -------------------------------------- |
| Syntax      | `$ARISA_LIST_USER_ACTIVITY <username>` |
| Permissions | Mod+                                   |

List all recent activity (up to 50 items) by the given user in a new mod+ comment.

## $ARISA_PURGE_ATTACHMENT
| Entry       | Value                                                      |
| ----------- | ---------------------------------------------------------- |
| Syntax      | `$ARISA_PURGE_ATTACHMENT <username> [<min ID>] [<max ID>]` |
| Permissions | Mod+                                                       |

Deletes all attachments on the ticket by user with the username if the attachment ID is between min ID and max ID.

Without specifying min/max ID, all attachments by that user are deleted.

Note: the numeric ID of an attachment can be found in the change log or in the link of it.

## $ARISA_REMOVE_COMMENTS
| Entry       | Value                               |
| ----------- | ----------------------------------- |
| Syntax      | `$ARISA_REMOVE_COMMENTS <username>` |
| Permissions | Mod+                                |

Finds all comments created by the user on a specific report and restricts them

Note: the command is successful regardless of whether any comments by that user exist

## $ARISA_REMOVE_CONTENT
| Entry       | Value                              |
| ----------- | ---------------------------------- |
| Syntax      | `$ARISA_REMOVE_CONTENT <username>` |
| Permissions | Mod+                               |

(Formerly `$ARISA_REMOVE_USER`)

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
