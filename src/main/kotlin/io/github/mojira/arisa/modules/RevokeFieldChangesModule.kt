package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2

class RevokeFieldChangesModule : Module<RevokeFieldChangesModule.Request> {
    data class Field(
        val id: String,
        val name: String,
        val value: String?,
        val defaultValue: String?,
        val groups: List<String>,
        val message: String?,
        val set: (String?) -> Either<Throwable, Unit>
    )

    data class ChangeLogItem(
        val field: String,
        val newValue: String?,
        val created: Long,
        val getAuthorGroups: () -> List<String>?
    )

    data class Request(
        val lastRun: Long,
        val fields: List<Field>,
        val changeLog: List<ChangeLogItem>,
        val addComment: (body: String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val changesToRevoke = fields
                .map {
                    it to changeLog
                        .filter(::isFieldChange.partially2(it.name))
                        .lastOrNull(::changedByVolunteer.partially2(lastRun).partially2(it.groups))
                        ?.newValue.getOrDefault(it.defaultValue)
                }
                    .filter { (field, value) ->
                        value.getOrDefault(field.defaultValue) != field.value.getOrDefault(field.defaultValue)
                    }
                    .flatMap{ (field, value) ->
                        listOfNotNull(
                            field.set.partially1(value),
                            if (!field.message.isNullOrBlank()) addComment.partially1(field.message) else null
                        )
                    }

            assertNotEmpty(changesToRevoke).bind()
            tryRunAll(changesToRevoke).bind()
        }
    }

    private fun isFieldChange(item: ChangeLogItem, field: String) =
        item.field == field

    private fun changedByVolunteer(item: ChangeLogItem, lastRun: Long, groups: List<String>) =
        !updateIsRecent(item, lastRun) || item.getAuthorGroups()?.any { it in groups } ?: true

    private fun updateIsRecent(item: ChangeLogItem, lastRun: Long) =
        item.created > lastRun

    private fun String?.getOrDefault(default: String?) =
        if (isNullOrBlank() || this == "null")
            default
        else
            this
}
