plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    swaps["my_swap"] = when(current.version) {
        "1" -> "\"Hello"
        "2" -> "\"Bye"
        else -> error("Unsupported version")
    }
}