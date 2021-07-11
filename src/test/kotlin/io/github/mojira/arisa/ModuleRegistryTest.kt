package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.ProjectCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.Module
import io.github.mojira.arisa.registry.ModuleRegistry
import io.github.mojira.arisa.registry.getModuleRegistries
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.mockk
import org.reflections.Reflections

val CONFIG = Config { addSpec(Arisa) }
    .from.yaml.watchFile("config/config.yml")
    .from.map.flat(
        mapOf(
            // Overwrite these settings to ensure we're always covering both test cases
            "arisa.modules.attachment.enabled" to "true",

            // Required credentials
            "arisa.credentials.username" to "test",
            "arisa.credentials.password" to "test",
            "arisa.credentials.dandelionToken" to "test"
        )
    )

class ModuleRegistryTest : StringSpec({
    "should register a module for each config class" {
        val modules = getAllModules().map { it.name }
        val configs = Arisa.Modules::class.java.classes.map { it.simpleName }.filter { !it.endsWith("Spec") }
        println("Configs not mapped to a registered module " + configs.filter { !modules.contains(it) })
        println("Registered modules not mapped to a config " + modules.filter { !configs.contains(it) })
        configs shouldContainExactlyInAnyOrder modules
    }

    "should register a module for each non-abstract module" {
        val classes = Reflections("io.github.mojira.arisa.modules")
            .getSubTypesOf(Module::class.java)
            .map { it.simpleName }
            .filter { !it.startsWith("Abstract") }
            .map { it.replace("Module", "") }
        val modules = getAllModules()
            .map { it.name }

        println("Classes not mapped to a registered module " + classes.filter { !modules.contains(it) })
        println("Registered modules not mapped to a class " + modules.filter { !classes.contains(it) })
        classes shouldContainExactlyInAnyOrder modules
    }

    "should register each config not more than once" {
        val modules = getAllModules().map { it.config.prefix }
        val uniqueModules = modules.distinct()
        println("Not unique modules " + modules.filter { !uniqueModules.contains(it) })
        uniqueModules shouldContainExactlyInAnyOrder modules
    }

    "should disable modules that aren't enabled and enable modules that aren't disabled" {
        val enabledModules = getEnabledModules().map { it.name }

        enabledModules shouldContain "Attachment"
    }
})

private fun getAllModules(): List<ModuleRegistry.Entry> {
    val projectCache = mockk<ProjectCache>()
    return getModuleRegistries(CONFIG, projectCache).flatMap { it.getAllModules() }
}

private fun getEnabledModules(): List<ModuleRegistry.Entry> {
    val projectCache = mockk<ProjectCache>()
    return getModuleRegistries(CONFIG, projectCache).flatMap { it.getEnabledModules() }
}
