package dev.kikugie.stonecutter

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPEALIAS

@Target(CLASS, TYPEALIAS, FUNCTION, PROPERTY, CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("This API is in development and may receive incompatible changes.", RequiresOptIn.Level.WARNING)
public annotation class StonecutterExperimentalAPI
