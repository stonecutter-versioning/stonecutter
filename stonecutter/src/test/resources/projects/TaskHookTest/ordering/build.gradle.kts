tasks.register("printVersion") {
    val version = stonecutter.current.version

    doFirst { println("My version is ${version}") }
}