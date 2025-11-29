plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    shared {
        versions("1", "2")
    }

    create(rootProject) {
        shared.execute(this)
    }
}