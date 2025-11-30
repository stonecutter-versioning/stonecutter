import dev.kikugie.stitcher.transform.impl.LineCommentStrategy

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1"

stonecutter handlers {
    configure("java") {
        commenter = LineCommentStrategy("//")
    }
}