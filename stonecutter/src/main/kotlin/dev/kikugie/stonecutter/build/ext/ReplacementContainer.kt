package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stitcher.transform.replacement.RegexReplacement
import dev.kikugie.stitcher.transform.replacement.StringReplacement
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ReplacementDsl

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ReplacementSpecDsl

private fun ReplacementContainer.StringReplacementSpec.build(): StringReplacement {
    require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
    return if (direction()) StringReplacement(to(), from(), identifier = id.orNull)
    else StringReplacement(from(), to(), identifier = id.orNull)
}

private fun ReplacementContainer.RegexReplacementSpec.build(): RegexReplacement {
    require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
    return if (direction()) RegexReplacement(toValue(), fromPattern(), identifier = id.orNull)
    else RegexReplacement(reverseValue(), reversePattern(), identifier = id.orNull)
}

@ReplacementDsl
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

    @ReplacementSpecDsl
    public interface StringReplacementSpec {
        public val direction: Property<Boolean>
        public val id: Property<Identifier>
        public val from: Property<String>
        public val to: Property<String>

        public fun replace(from: String, to: String) {
            this.from.value(from).disallowChanges()
            this.to.value(to).disallowChanges()
        }
    }

    @ReplacementSpecDsl
    public interface RegexReplacementSpec {
        public val direction: Property<Boolean>
        public val id: Property<Identifier>
        public val fromPattern: Property<String>
        public val toValue: Property<String>
        public val reversePattern: Property<String>
        public val reverseValue: Property<String>

        public fun replace(from: String, to: String) {
            fromPattern.value(from).disallowChanges()
            toValue.value(to).disallowChanges()
        }

        public fun reverse(from: String, to: String) {
            reversePattern.value(from).disallowChanges()
            reverseValue.value(to).disallowChanges()
        }
    }

    private open class Impl(
        val objects: ObjectFactory,
        val string: (StringReplacement) -> Unit,
        val regex: (RegexReplacement) -> Unit,
    ) : ReplacementContainer {
        override fun string(action: Action<StringReplacementSpec>): Unit =
            objects.newInstance<StringReplacementSpec>(action).build().let(string)

        override fun regex(action: Action<RegexReplacementSpec>): Unit =
            objects.newInstance<RegexReplacementSpec>(action).build().let(regex)
    }

    @StonecutterInternalAPI
    public companion object {
        internal operator fun invoke(objects: ObjectFactory, string: (StringReplacement) -> Unit, regex: (RegexReplacement) -> Unit): ReplacementContainer =
            Impl(objects, string, regex)
    }
}
