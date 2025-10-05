plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject, file("versions.json5")) {
        version("snapshot", "1.21.9")
    }
}