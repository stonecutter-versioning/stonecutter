package dev.kikugie.stitcher.debug

internal inline fun debugView(name: String, builder: DebugView.() -> Unit = {}): DebugView =
    DebugView(name).apply(builder)

internal data class DebugView(
    val name: String,
    var properties: Map<String, String> = emptyMap(),
    var attributes: Map<String, DebugView> = emptyMap(),
    var items: List<DebugView> = emptyList()
)