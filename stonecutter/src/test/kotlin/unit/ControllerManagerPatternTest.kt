package unit

import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Groovy
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Kotlin
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.stable.IsStableType
import io.kotest.matchers.shouldBe

@IsStableType
private data class ReplaceSpec(val controller: StonecutterControllerManager, val input: String, val template: String, val output: String) {
    override fun toString(): String = "{$input} ~ '$template'"
    fun run() = controller.replace(input, template)
}

class ControllerManagerPatternTest : FunSpec({
    context("groovy") {
        withData(
            ReplaceSpec(Groovy, """stonecutter.active("1.0")""", "2.0", """stonecutter.active("2.0")"""),
            ReplaceSpec(Groovy, """stonecutter.active ('1.0')""", "2.0", """stonecutter.active ('2.0')"""),
            ReplaceSpec(Groovy, """stonecutter . active "1.0"""", "2.0", """stonecutter . active "2.0""""),
            ReplaceSpec(Groovy, """stonecutter .active '1.0'""", "2.0", """stonecutter .active '2.0'"""),
            ReplaceSpec(Groovy, """sc.active ( "1.0" )""", "2.0", """sc.active ( "2.0" )"""),
            ReplaceSpec(Groovy, """sc.active ('1.0')""", "2.0", """sc.active ('2.0')"""),
            ReplaceSpec(Groovy, """sc . active "1.0"""", "2.0", """sc . active "2.0""""),
            ReplaceSpec(Groovy, """sc .active '1.0'""", "2.0", """sc .active '2.0'"""),
        ) {
            it.run() shouldBe it.output
        }
    }

    context("kotlin") {
        withData(
            ReplaceSpec(Kotlin, """stonecutter . active ("1.0")""", "2.0", """stonecutter . active ("2.0")"""),
            ReplaceSpec(Kotlin, """stonecutter active "1.0"""", "2.0", """stonecutter active "2.0""""),
            ReplaceSpec(Kotlin, """sc.active ( "1.0" )""", "2.0", """sc.active ( "2.0" )"""),
            ReplaceSpec(Kotlin, """sc active "1.0"""", "2.0", """sc active "2.0""""),
        ) {
            it.run() shouldBe it.output
        }
    }
})