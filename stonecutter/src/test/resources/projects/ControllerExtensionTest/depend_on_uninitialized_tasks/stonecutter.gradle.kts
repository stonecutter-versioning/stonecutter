plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active null

tasks.register("sayHelloAndGoodbye") {
    doLast { println("Goodbye!") }
    dependsOn(stonecutter.tasks.named("sayHello"))
}