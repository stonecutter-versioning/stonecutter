tasks.register("printVersion") {
    doFirst { println("My version is ${stonecutter.current.version}") }
}