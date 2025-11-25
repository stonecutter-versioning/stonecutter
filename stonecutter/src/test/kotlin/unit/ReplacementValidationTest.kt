@file:Suppress("NOTHING_TO_INLINE")

package unit

import dev.kikugie.stitcher.transform.replacement.RegexReplacement
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stitcher.transform.replacement.ReplacementBuilder
import dev.kikugie.stitcher.transform.replacement.StringReplacement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.intellij.lang.annotations.Language

private inline fun replacements(action: ReplacementBuilder<Replacement>.() -> Unit) =
    ReplacementBuilder().apply(action).build()

private inline fun ReplacementBuilder<Replacement>.string(from: String, to: String, id: String? = null) =
    add(StringReplacement(to, from, identifier = id))

private inline fun ReplacementBuilder<Replacement>.regex(@Language("RegExp") from: String, to: String, id: String? = null) =
    add(RegexReplacement(to, from, identifier = id))

class ReplacementValidationTest : FunSpec({
    context("generic") {
        test("empty inputs") {
            shouldThrow<IllegalArgumentException> { replacements { string("a", "") } }
            shouldThrow<IllegalArgumentException> { replacements { string("", "a") } }
            shouldThrow<IllegalArgumentException> { replacements { regex("", "a") } }
            shouldThrow<IllegalArgumentException> { replacements { regex("a", "") } }
        }

        test("existing id") {
            replacements {
                string("a", "b", "id")
                regex("a", "b", "id")
            }
        }
    }

    context("string") {
        test("composite") {
            val repls = replacements {
                string("a", "c")
                string("b", "c")
            }

            repls.single().shouldBeInstanceOf<StringReplacement> {
                it.sources shouldContainAll setOf("a", "b")
                it.target shouldBe "c"
            }
        }

        test("transitive") {
            val repls = replacements {
                string("a", "b")
                string("b", "c")
            }

            repls.single().shouldBeInstanceOf<StringReplacement> {
                it.sources shouldContainAll setOf("a", "b")
                it.target shouldBe "c"
            }
        }

        test("cyclical") {
            shouldThrow<IllegalArgumentException> {
                replacements {
                    string("a", "b")
                    string("b", "c")
                    string("c", "a")
                }
            }
        }

        test("ambiguous") {
            shouldThrow<IllegalArgumentException> {
                replacements {
                    string("a", "b")
                    string("a", "c")
                }
            }
        }
    }
})