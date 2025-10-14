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
            id = "my_replacement"
            replace("Hello World!", "!dlroW olleH")
        }
    }
}