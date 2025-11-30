plugins {
    java
    application
}

application {
    mainClass = "Example"
}

stonecutter {
    constants["my_constant"] = eval(current.version, ">1")
}