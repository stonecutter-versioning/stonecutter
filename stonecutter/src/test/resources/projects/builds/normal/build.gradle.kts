tasks.register("printCurrentProject") {
    doFirst { println(stonecutter.current.version) }
}