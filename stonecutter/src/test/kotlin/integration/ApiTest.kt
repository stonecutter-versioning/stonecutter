package integration

import gradle.GradleTest
import gradle.minus
import io.kotest.core.spec.style.FreeSpec

class ApiTest : GradleTest, FreeSpec({
    "parsed version" - { directory, build ->
        build.run()
    }
})