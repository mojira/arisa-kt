package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Attachment

class AttachmentModule(
    private val extensionBlackList: List<String>
) : Module<AttachmentModule.Request> {

    data class Request(
        val attachmentsToDeleteFunction: List<Attachment>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val endsWithBlacklistedExtensionAdapter = ::endsWithBlacklistedExtensions.partially1(extensionBlackList)
            val functions = attachmentsToDeleteFunction
                .filter { (name, _) -> endsWithBlacklistedExtensionAdapter(name) }
                .map { it.remove }
            assertNotEmpty(functions).bind()
            tryRunAll(functions).bind()
        }
    }

    private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, name: String) =
        extensionBlackList.any { name.endsWith(it) }
}
