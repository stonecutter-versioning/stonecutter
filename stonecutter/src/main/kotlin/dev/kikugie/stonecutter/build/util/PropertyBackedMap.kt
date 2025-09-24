package dev.kikugie.stonecutter.build.util

import dev.kikugie.commons.then
import dev.kikugie.stonecutter.util.get
import dev.kikugie.stonecutter.util.orEmpty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.annotations.Contract

@Suppress("UNCHECKED_CAST")
internal open class PropertyBackedMap<K : Any, V : Any>(private val factory: ProviderFactory, internal val property: MapProperty<K, V>)
    : DynamicMap<K, V> {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = property.orEmpty().entries as MutableSet<MutableMap.MutableEntry<K, V>>
    override val keys: MutableSet<K>
        get() = property.orEmpty().values as MutableSet<K>
    override val values: MutableCollection<V>
        get() = property.orEmpty().values as MutableCollection<V>
    override val size: Int
        get() = property.orEmpty().size

    override fun clear(): Unit = property.set(mutableMapOf())
    override fun isEmpty(): Boolean = property.orEmpty().isEmpty()
    override fun get(key: K): V? = property[key].orNull

    override fun set(key: K, value: V): Unit = checkBoth(key, value) then property.put(key, value)
    override fun set(key: K, supplier: () -> V): Unit = checkKey(key) then set(key, factory.provider(supplier))
    override fun set(key: K, provider: Provider<V>): Unit = checkKey(key) then property.put(key, provider.asChecked())

    override fun put(key: K, value: V): V? = withPreviousValue(key) { set(key, value) }
    override fun put(key: K, supplier: () -> V): V? = withPreviousValue(key) { set(key, supplier) }
    override fun put(key: K, provider: Provider<V>): V? = withPreviousValue(key) { set(key, provider) }

    override fun putAll(from: Map<out K, V>): Unit = from.forEach { (k, v) -> checkBoth(k, v) } then property.putAll(from)

    override fun containsKey(key: K): Boolean = key in property.orEmpty()
    override fun containsValue(value: V): Boolean = property.orEmpty().containsValue(value)

    @Throws(UnsupportedOperationException::class) @Contract("_ -> fail")
    override fun remove(key: K): V? = throw UnsupportedOperationException(
        "Removal of arbitrary elements is not supported for Gradle MapProperty"
    )

    protected open fun checkKey(key: K) {}
    protected open fun checkValue(value: V) {}
    private fun checkBoth(key: K, value: V) { checkKey(key); checkValue(value) }
    private inline fun withPreviousValue(key: K, action: () -> Unit): V? = property[key].orNull.also { action() }
    private fun Provider<V>.asChecked() = map { checkValue(it); it }
}