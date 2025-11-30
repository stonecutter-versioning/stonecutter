package dev.kikugie.stonecutter

/**
 * A [String] that should represent a valid identifier,
 * matching the pattern `[_a-zA-Z][_\-a-zA-Z0-9]*`.
 *
 * This type is used for naming subrpojects and parameters
 * like constants, swaps, etc.
 */
public typealias Identifier = String

/**
 * A [String] that should represent either a valid [SemanticVersion][dev.kikugie.semver.data.SemanticVersion]
 * or [StringVersion][dev.kikugie.semver.data.StringVersion].
 *
 * TODO: Add wiki link
 */
public typealias AnyVersion = String

/**
 * Represents either a [String] or [ProjectDescriptor][org.gradle.api.initialization.ProjectDescriptor].
 */
public typealias ProjectReference = Any