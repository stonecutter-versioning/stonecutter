package dev.kikugie.stonecutter.controller.file

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPEALIAS

@RequiresOptIn("This API is in development. Expect major changes.", RequiresOptIn.Level.WARNING)
@Target(CLASS, TYPEALIAS, FUNCTION, PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class StonecutterExperimentalFilesAPI
