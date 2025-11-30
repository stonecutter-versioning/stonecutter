plugins {
    id("dev.kikugie.stonecutter")
}

sc active "example"

stonecutter flags {
    this["auto_apply_plugin"] = false
}