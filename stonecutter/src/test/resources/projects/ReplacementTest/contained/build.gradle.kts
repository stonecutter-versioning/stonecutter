plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    replacements {
        string(current.version == "2") {
            replace("prefix.a.suffix", "prefix.a.suffix.b")
        }
    }
}