plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        vcsVersion = "1"
        versions("3", "1", "2")
    }
}