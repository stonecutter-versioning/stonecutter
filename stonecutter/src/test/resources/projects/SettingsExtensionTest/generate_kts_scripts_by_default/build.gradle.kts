tasks.register("hello") {
    val version = sc.current.version
    doLast { println("$version!") }
}