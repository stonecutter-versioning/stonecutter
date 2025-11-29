@file:OptIn(ExperimentalSerializationApi::class)

package unit

import dev.kikugie.stonecutter.settings.tree.SerializedTree
import dev.kikugie.stonecutter.settings.tree.SerializedVersion
import dev.kikugie.stonecutter.settings.tree.TreeScheme
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import util.shouldHaveAt

private val JSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    allowComments = true
    allowTrailingComma = true
}

class TreeSerializationTest : FunSpec({
    context("project notation") {
        fun deserialize(input: String) =
            JSON.decodeFromString(SerializedVersion.PrimitiveJsonSerializer, "\"$input\"")

        test("simple") {
            with(deserialize("1.21.1")) {
                project shouldBe version shouldBe "1.21.1"
                buildscript shouldBe null
            }
        }

        test("with version") {
            with(deserialize("1.21.1-fabric:1.21.1")) {
                project shouldBe "1.21.1-fabric"
                version shouldBe "1.21.1"
                buildscript shouldBe null
            }
        }

        test("with buildscript") {
            with(deserialize("1.21.1-fabric:1.21.1:fabric.gradle.kts")) {
                project shouldBe "1.21.1-fabric"
                version shouldBe "1.21.1"
                buildscript shouldBe "fabric.gradle.kts"
            }
        }

        test("default with buildscript") {
            with(deserialize("1.21.1::fabric.gradle.kts")) {
                project shouldBe version shouldBe "1.21.1"
                buildscript shouldBe "fabric.gradle.kts"
            }
        }
    }

    context("tree types") {
        fun deserialize(@Language("JSON5") input: String) =
            JSON.decodeFromString(SerializedTree.TreeJsonSerializer, input)

        test("version list") {
            val tree = deserialize("""
                {
                  versions: [ "example", "1.21.1" ]
                }
            """.trimIndent())

            (tree.schemes shouldHaveAt 0).shouldBeInstanceOf<TreeScheme.Plain> {
                (it.versions shouldHaveAt 0).project shouldBe "example"
                (it.versions shouldHaveAt 1).project shouldBe "1.21.1"
            }
        }

        test("branch version list") {
            val tree = deserialize("""
                {
                  branches: { "": [ "example", "1.21.1" ] }
                }
            """.trimIndent())

            (tree.schemes shouldHaveAt 0).shouldBeInstanceOf<TreeScheme.Branched> {
                val root = it.branches shouldHaveAt ""

                (root shouldHaveAt 0).project shouldBe "example"
                (root shouldHaveAt 1).project shouldBe "1.21.1"
            }
        }

        test("branch version map") {
            val tree = deserialize("""
                {
                  branches: { "": { versions: [ "example", "1.21.1" ] } }
                }
            """.trimIndent())

            (tree.schemes shouldHaveAt 0).shouldBeInstanceOf<TreeScheme.Branched> {
                val root = it.branches shouldHaveAt ""

                (root shouldHaveAt 0).project shouldBe "example"
                (root shouldHaveAt 1).project shouldBe "1.21.1"
            }
        }

        test("version branch list") {
            val tree = deserialize("""
                {
                  versions: { "example": ["", "subproject"] }
                }
            """.trimIndent())

            (tree.schemes shouldHaveAt 0).shouldBeInstanceOf<TreeScheme.Inverted> {
                val root = it.versions shouldHaveAt SerializedVersion("example")

                root shouldHaveAt 0 shouldBe ""
                root shouldHaveAt 1 shouldBe "subproject"
            }
        }

        test("version branch map") {
            val tree = deserialize("""
                {
                  versions: { example: { branches: ["", "subproject"] } }
                }
            """.trimIndent())

            (tree.schemes shouldHaveAt 0).shouldBeInstanceOf<TreeScheme.Inverted> {
                val root = it.versions shouldHaveAt SerializedVersion("example")

                root shouldHaveAt 0 shouldBe ""
                root shouldHaveAt 1 shouldBe "subproject"
            }
        }
    }
})
