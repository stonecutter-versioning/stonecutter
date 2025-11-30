plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    swaps["my_swap"] = when(current.version) {
        "1" -> "\"Hello Lace!"
        "2" -> "\"Bye Tim!"
        else -> error("Unsupported version")
    }
}