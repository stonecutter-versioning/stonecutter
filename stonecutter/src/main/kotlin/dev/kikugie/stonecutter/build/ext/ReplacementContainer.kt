package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stitcher.transform.replacement.RegexReplacement
import dev.kikugie.stitcher.transform.replacement.StringReplacement
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ReplacementDsl

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ReplacementSpecDsl

@StonecutterAPI @ReplacementDsl
public sealed interface ReplacementContainer {
    public fun string(action: Action<StringReplacementSpec>)

    public fun string(direction: Boolean, action: Action<StringReplacementSpec>): Unit =
        string { this.direction.set(direction); action.execute(this) }

    public fun string(id: Identifier, direction: Boolean? = null, action: Action<StringReplacementSpec>): Unit =
        string { this.id.set(id); this.direction.set(direction); action.execute(this) }

    public fun regex(action: Action<RegexReplacementSpec>)

    public fun regex(direction: Boolean, action: Action<RegexReplacementSpec>): Unit =
        regex { this.direction.set(direction); action.execute(this) }

    public fun regex(id: Identifier, direction: Boolean? = null, action: Action<RegexReplacementSpec>): Unit =
        regex { this.id.set(id); this.direction.set(direction); action.execute(this) }

    @StonecutterAPI @ReplacementSpecDsl
    public interface StringReplacementSpec {
        public val direction: Property<Boolean>
        public val id: Property<Identifier>
        public val from: Property<String>
        public val to: Property<String>

        public fun replace(from: String, to: String)
    }

    @StonecutterAPI @ReplacementSpecDsl
    public interface RegexReplacementSpec {
        public val direction: Property<Boolean>
        public val id: Property<Identifier>
        public val fromPattern: Property<String>
        public val toValue: Property<String>
        public val reversePattern: Property<String>
        public val reverseValue: Property<String>

        @Deprecated("Use the combined function")
        public fun replace(from: String, to: String) {
            fromPattern.value(from).disallowChanges()
            toValue.value(to).disallowChanges()
        }

        @Deprecated("Use the combined function")
        public fun reverse(from: String, to: String) {
            reversePattern.value(from).disallowChanges()
            reverseValue.value(to).disallowChanges()
        }

        public fun replace(direct: Pair<String, String>, reverse: Pair<String, String>)

        public fun replace(directPattern: String, directReplacement: String, reversePattern: String, reverseReplacement: String): Unit =
            replace(directPattern to directReplacement, reversePattern to reverseReplacement)
    }

    private open class Impl(
        val objects: ObjectFactory,
        val string: (StringReplacement) -> Unit,
        val regex: (RegexReplacement) -> Unit,
    ) : ReplacementContainer {
        override fun string(action: Action<StringReplacementSpec>): Unit =
            objects.newInstance<StringSpecImpl>(string).also(action::execute).finalize()

        override fun regex(action: Action<RegexReplacementSpec>): Unit =
            objects.newInstance<RegexSpecImpl>(regex).also(action::execute).finalize()
    }

    @StonecutterInternalAPI
    public companion object {
        internal operator fun invoke(objects: ObjectFactory, string: (StringReplacement) -> Unit, regex: (RegexReplacement) -> Unit): ReplacementContainer =
            Impl(objects, string, regex)
    }
}

internal abstract class StringSpecImpl @Inject constructor(val consumer: (StringReplacement) -> Unit) : ReplacementContainer.StringReplacementSpec {
    override fun replace(from: String, to: String) {
        require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
        val replacement =
            if (direction()) StringReplacement(to, from, identifier = id.orNull)
            else StringReplacement(from, to, identifier = id.orNull)
        consumer(replacement)
    }

    fun finalize() {
        if (from.isPresent || to.isPresent) replace(from(), to())
    }
}

internal abstract class RegexSpecImpl @Inject constructor(val consumer: (RegexReplacement) -> Unit) : ReplacementContainer.RegexReplacementSpec {
    override fun replace(direct: Pair<String, String>, reverse: Pair<String, String>) {
        require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
        val replacement =
            if (direction()) RegexReplacement(direct.first, direct.second, identifier = id.orNull)
            else RegexReplacement(reverse.first, reverse.second, identifier = id.orNull)
        consumer(replacement)
    }

    fun finalize() {
        if (fromPattern.isPresent || toValue.isPresent || reversePattern.isPresent || reverseValue.isPresent)
            replace(fromPattern() to toValue(), reversePattern() to reverseValue())
    }
}