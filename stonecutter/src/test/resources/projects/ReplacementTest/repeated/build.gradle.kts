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
            replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
            replace("net/minecraft/resources/ResourceLocation", "net/minecraft/resources/Identifier")
        }
    }
}