/*
 * Implementation is based on
 * https://github.com/uchuhimo/konf/blob/8aa88358b89f7ef2b124ee608b852a18a43aac7f/konf-core/src/main/kotlin/com/uchuhimo/konf/Spec.kt#L190-L295
 */

/**
 * Provides custom [com.uchuhimo.konf.RequiredItem] subclasses to allow differentiating config items which refer to
 * helper message keys.
 */

package io.github.mojira.arisa.infrastructure.config

import com.fasterxml.jackson.databind.type.TypeFactory
import com.uchuhimo.konf.RequiredItem
import com.uchuhimo.konf.Spec
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Specify a required helper message key item in this config spec.
 *
 * @param name item name without prefix
 * @param description description for this item
 *
 * @return a property of a required item with prefix of this config spec
 */
fun Spec.requiredMessageKey(name: String? = null, description: String = "") =
    RequiredMessageProperty(this, name, description)

private val stringType = TypeFactory.defaultInstance().constructType(String::class.java)

/** Item representing the key of a helper message. */
class MessageKeyItem(
    spec: Spec,
    name: String,
    description: String
) : RequiredItem<String>(spec, name, description, stringType, false)

class RequiredMessageProperty(
    private val spec: Spec,
    private val name: String? = null,
    private val description: String = ""
) {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, RequiredItem<String>> {
        val item = MessageKeyItem(spec, name ?: property.name, description)
        return object : ReadOnlyProperty<Any?, RequiredItem<String>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = item
        }
    }
}

/** Item representing a map from keys of type `K` to keys of helper messages. */
class MessageKeyMapItem<K>(
    spec: Spec,
    name: String,
    description: String,
    keyType: Class<out K>
) : RequiredItem<Map<K, String>>(
    spec,
    name,
    description,
    // Type for Map<K, String>
    TypeFactory.defaultInstance().constructMapType(Map::class.java, keyType, String::class.java),
    false
)

/**
 * Specify a required item in this config spec, representing a map from keys of type `K` to keys of helper messages.
 *
 * @param K type of the keys
 * @param name item name without prefix
 * @param description description for this item
 *
 * @return a property of a required item with prefix of this config spec
 */
inline fun <reified K> Spec.requiredMessageKeyMap(name: String? = null, description: String = "") =
    RequiredMessageMapProperty(this, name, description, K::class.java)

class RequiredMessageMapProperty<K>(
    private val spec: Spec,
    private val name: String? = null,
    private val description: String = "",
    private val keyType: Class<out K>
) {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, MessageKeyMapItem<K>> {
        val item = MessageKeyMapItem(
            spec,
            name ?: property.name,
            description,
            keyType
        )
        return object : ReadOnlyProperty<Any?, MessageKeyMapItem<K>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): MessageKeyMapItem<K> = item
        }
    }
}
