plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    kotlinController = false

    create(rootProject) {
        versions("example")
    }
}