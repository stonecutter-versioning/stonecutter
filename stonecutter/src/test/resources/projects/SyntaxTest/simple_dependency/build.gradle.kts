plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    dependencies["my_dependency"] = when(current.version) {
        "1" -> "2"
        "2" -> "1"
        else -> error("Unsupported version")
    }
}