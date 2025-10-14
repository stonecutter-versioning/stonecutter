plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        versions("1.20.1", "1.21.1", "1.21.6")
        vcsVersion = "1.21.6"
    }
}