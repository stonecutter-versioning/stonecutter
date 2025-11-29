plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        version("it" to "1")
        branch("subproject") {
            version("it" to "2")
        }
    }
}