plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        versions("1.20.1", "1.21.1")
        version("snapshot", "1.21.9-alpha.25.39")
    }
}