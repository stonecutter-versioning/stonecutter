plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        inherit()
        versions("1", "2")
    }
}