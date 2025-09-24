package dev.kikugie.stonecutter.controller.flag

import dev.kikugie.stonecutter.StonecutterAPI

@StonecutterAPI
public data class StonecutterFlag<T : Any>(public val key: String, public val default: T) {
    init {
        require(key.isNotBlank()) { "Flag key cannot be blank" }
        require(key.all { it in 'a'..'z' || it == '_' }) { "Flag key must be written in snake case" }
        require(default.isPrimitive()) { "Flag value must be a primitive type, got ${default::class.simpleName} instead" }
        check(REGISTRY.putIfAbsent(key, this) == null) { "Flag '$key' is already registered" }
    }

    @Suppress("UNCHECKED_CAST")
    public fun fromString(value: String): T = when (default) {
        is String -> value
        is Boolean -> value.toBoolean()
        is Int -> value.toInt()
        is Long -> value.toLong()
        is Float -> value.toFloat()
        is Double -> value.toDouble()
        else -> UnsupportedOperationException("Unreachable")
    } as T

    public companion object {
        private val REGISTRY: MutableMap<String, StonecutterFlag<*>> = mutableMapOf()
        private fun Any.isPrimitive(): Boolean = when (this) {
            is Boolean, is Int, is Long, is Float, is Double, is String -> true
            else -> false
        }

        @JvmStatic public fun named(name: String): StonecutterFlag<*> =
            checkNotNull(REGISTRY[name]) { "Flag '$name' is not registered" }

        /**
         * Configures the automatic plugin application behaviour.
         * When disabled, the Stonecutter plugin will not be automatically applied
         * to versioned `build.gradle[.kts]` files, and will have to be added explicitly with
         * ```kotlin
         * plugins {
         *     id("dev.kikugie.stonecutter")
         * }
         * ```
         * **This only has effect if configured before the `stonecutter.active(...)` call.**
         *
         * **Default**: `true`
         */
        @JvmField
        public val APPLY_PLUGIN_TO_NODES: StonecutterFlag<Boolean> = StonecutterFlag("auto_apply_plugin", true)

        /**
         * Configures versioned source generation on IntelliJ sync.
         * When disabled, sourced will only be updated when the project is built.
         *
         * **Default**: `true`
         */
        @JvmField
        public val GENERATE_SOURCES_ON_SYNC: StonecutterFlag<Boolean> = StonecutterFlag("generate_sources_on_sync", true)

        /**
         * Configures run configuration generation in IntelliJ.
         * When enabled, version switch tasks will be available per project in the
         * "Run configurations" dropdown menu.
         *
         * **Default**: `true`
         */
        @JvmField
        public val GENERATE_SWITCH_ACTIONS: StonecutterFlag<Boolean> = StonecutterFlag("generate_switch_actions", true)

        /**
         * Configures the source generation mode for [dev.kikugie.stonecutter.build.StonecutterBuildExtension].
         * Normally, the build plugin won't detect source directories added after the source set creation.
         * With this flag enabled, it will do a second pass after project evaluation.
         * Disable if it causes issues and append source sets manually with the help of [dev.kikugie.stonecutter.build.task.StonecutterBuildTasks].
         *
         * **Default**: `true`
         */
        @JvmField
        public val APPEND_SOURCES_AFTER_EVAL: StonecutterFlag<Boolean> = StonecutterFlag("extra_source_check", true)

        @JvmField
        public val SERIALIZE_TREE_MODEL: StonecutterFlag<Boolean> = StonecutterFlag("serialize_tree_model", true)

        /**
         * Configures the implicit receiver target used in file processing.
         * For more information on this functionality refer to the wiki.
         *
         * **Default**: `"minecraft"`
         */
        @JvmField
        public val IMPLICIT_RECEIVER: StonecutterFlag<String> = StonecutterFlag("implicit_receiver", "minecraft")
    }
}
