package dev.kikugie.stonecutter.data.version

import java.io.File

public interface FileOperations {
    /**
     * Processes the [file] using existing handlers and configuration,
     * writing the result to [destination] relative to the project directory.
     * @return The processed file
     */
    public fun process(file: File, destination: String): File
}