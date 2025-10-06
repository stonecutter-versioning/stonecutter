plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active null

if (hasProperty("order-prints")) stonecutter tasks {
    order("printVersion")
}