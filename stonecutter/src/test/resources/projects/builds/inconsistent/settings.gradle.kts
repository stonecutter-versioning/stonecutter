plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        version("snapshot", "1.21.9-alpha.25.39")
        branch("example") {
            version("snapshot", "1.21.9-alpha.25.38")
        }
    }
}