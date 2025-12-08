plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        versions("1.14.4", "1.16.5", "1.21.4")
        vcsVersion = "1.21.4"
    }
}