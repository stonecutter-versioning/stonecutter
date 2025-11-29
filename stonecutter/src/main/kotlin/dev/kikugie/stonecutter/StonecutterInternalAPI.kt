package dev.kikugie.stonecutter

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPEALIAS

@RequiresOptIn("This API is internal to Stonecutter and should not be used.", RequiresOptIn.Level.WARNING)
@Target(CLASS, TYPEALIAS, FUNCTION, PROPERTY, CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
internal annotation class StonecutterInternalAPI
