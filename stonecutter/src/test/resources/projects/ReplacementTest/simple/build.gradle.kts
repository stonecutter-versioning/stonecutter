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
            replace("Hello World!", "!dlroW olleH")
        }
    }
}