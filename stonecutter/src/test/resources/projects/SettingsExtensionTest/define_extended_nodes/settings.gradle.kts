plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject, file("versions.json5"))
}