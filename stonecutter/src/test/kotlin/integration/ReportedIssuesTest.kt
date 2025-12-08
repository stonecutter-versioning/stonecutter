package integration

import gradle.GradleTest
import gradle.read
import gradle.should
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ReportedIssuesTest : GradleTest, ShouldSpec({
    should("not insert eof") { dir, build ->
        build.run("stonecutterSwitchTo2")
        build.run("stonecutterSwitchTo1")
        build.run(":1:run").output shouldContain "Hello world!"
        dir read "src/main/java/Example.java" shouldNotContain "<EOF>"
    }

    // https://codeberg.org/stonecutter/stonecutter/issues/18
    should("count nested comments") { dir, build ->
        build.run("stonecutterSwitchTo1.14.4")
        val file = dir read "src/main/java/PopupScreen.kt"
        val line = file.lines()[24]
        line shouldBe "      /*renderBlurredBackground(/*? if <=1.21.1 {*/partialTick/*?}*/)"
    }

    // https://codeberg.org/stonecutter/stonecutter/issues/19
    should("count insertion offsets") { dir, build ->
        build.run("stonecutterSwitchTo1.20.1")
        build.run("stonecutterSwitchTo1.21.6")
        val file = dir read "src/main/java/WaterFogEnvironmentMixin.java"
        val line = file.lines()[28]
        line shouldBe "    /*@SuppressWarnings(\"rawtypes\")*/"
    }

    // https://codeberg.org/stonecutter/stonecutter/issues/21
    should("preserve line break") { dir, build ->
        build.run("stonecutterSwitchTo1")
        val file = dir read "src/main/java/Example.java"
        val line = file.lines()[3]
        line shouldBe "        //System.out.println(\"Hello world!\");"
    }

    // TODO: Unfinished
    // https://codeberg.org/stonecutter/stonecutter/issues/22
    should("combine line scopes") { dir, build ->
        build.run("stonecutterSwitchTo1")
        dir read "src/main/java/Example.java" shouldBe """
            //? if >1
            /*//? if >2
            //public class Example { }*/
        """.trimIndent()
    }
})