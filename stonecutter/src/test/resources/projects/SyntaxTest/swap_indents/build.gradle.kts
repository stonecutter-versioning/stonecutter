plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    swaps["my_swap"] = when(current.version) {
        "1" -> "System.out.println(\"Hello Tim!\");"
        "2" -> "System.out.println(\"Hello Lace!\");"
        else -> error("Unsupported version")
    }
}