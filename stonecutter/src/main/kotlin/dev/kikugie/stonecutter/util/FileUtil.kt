package dev.kikugie.stonecutter.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

@OptIn(ExperimentalSerializationApi::class)
internal val SCJSON: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    allowComments = true
    allowTrailingComma = true
    prettyPrint = true
    prettyPrintIndent = "  "
}

internal fun File.overwriteText(text: CharSequence, createDirectories: Boolean = false): Unit =
    toPath().overwriteText(text, createDirectories)

internal fun Path.overwriteText(text: CharSequence, createDirectories: Boolean = false) {
    if (createDirectories) createParentDirectories()
    writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
}

internal fun Property<RegularFile>.toPath(): Path =
    get().asFile.toPath()