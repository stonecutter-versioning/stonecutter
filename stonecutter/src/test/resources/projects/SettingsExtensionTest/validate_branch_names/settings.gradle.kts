plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        versions("3", "1", "2")
        branch("!!!")
    }
}