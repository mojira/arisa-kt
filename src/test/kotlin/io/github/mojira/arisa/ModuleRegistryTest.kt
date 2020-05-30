package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.Module
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.reflections.Reflections

class ModuleRegistryTest : StringSpec({
    "should register a module for each config class" {
        val modules = ModuleRegistry(getConfig()).getModules().map { it.name }
        val configs = Arisa.Modules::class.java.classes.map { it.simpleName }.filter { !it.endsWith("Spec") }
        println("Configs not mapped to a registered module " + configs.filter { !modules.contains(it) })
        println("Registered modules not mapped to a config " + modules.filter { !configs.contains(it) })
        configs shouldContainExactlyInAnyOrder modules
    }

    "should register max 1 time each config" {
        val modules = ModuleRegistry(getConfig()).getModules().map { it.config.prefix }
        val uniqueModules = modules.distinct()
        println("Not unique modules " + modules.filter { !uniqueModules.contains(it) })
        uniqueModules shouldContainExactlyInAnyOrder modules
    }

    "should get only get a module with only" {
        val modules = ModuleRegistry(getConfig(true)).getModules()
        modules.size shouldBe 1
    }

    "should register a module for each non-abstract module" {
        val classes = Reflections("io.github.mojira.arisa.modules")
            .getSubTypesOf(Module::class.java)
            .map { it.simpleName }
            .filter { !it.startsWith("Abstract") }
            .map { it.replace("Module", "") }
        val modules = ModuleRegistry(getConfig()).getModules()
            .map { it.name }

        println("Classes not mapped to a registered module " + classes.filter { !modules.contains(it) })
        println("Registered modules not mapped to a class " + modules.filter { !classes.contains(it) })
        classes shouldContainExactlyInAnyOrder modules
    }
})

private fun getConfig(only: Boolean = false) = Config { addSpec(Arisa) }
    .from.json.watchFile("arisa.json")
    .from.map.flat(
        mapOf(
            "arisa.modules.attachment.only" to only.toString(),
            "arisa.credentials.username" to "test",
            "arisa.credentials.password" to "test",
            "arisa.credentials.dandelionToken" to "test"
        )
    )
