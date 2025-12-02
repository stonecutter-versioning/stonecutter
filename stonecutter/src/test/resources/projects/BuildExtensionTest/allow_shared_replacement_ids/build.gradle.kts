stonecutter {
    replacements {
        string(true) {
            id = "test"
            replace("a.b.c", "a.d.c")
        }
        string(false) {
            id = "test"
            replace("1.2.3", "1.3.4")
        }
    }
}