plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    swaps["my_swap"] = when(current.version) {
        "1" -> "System.out.println(\"Hello $1!\");"
        "2" -> "System.out.println(\"Bye $1!\");"
        else -> error("Unsupported version")
    }
}