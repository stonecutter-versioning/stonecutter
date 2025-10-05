plugins {
    id("dev.kikugie.stonecutter")
}

val props = gradle.startParameter.projectProperties

stonecutter {
    if ("top-groovy-controller" in props)
        kotlinController = false

    create(rootProject) {
        if ("tree-groovy-controller" in props)
            kotlinController = false
        versions("example")
    }
}