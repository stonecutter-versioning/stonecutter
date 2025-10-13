tasks.register("printCurrentProject") {
    val version = stonecutter.current.version

    doFirst { println(version) }
}