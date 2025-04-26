# Project settings

## Adding the plugin
Stonecutter is applied in the `settings.gradle[.kts]` file,
where you define, which versioned subprojects should be created.

::: tabs key:dsl
== settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.6-beta.3"
}

stonecutter {
    // Subproject configuration
}
```

== settings.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = "https://maven.kikugie.dev/releases" }
        maven { url = "https://maven.kikugie.dev/snapshots" }
    }
}

plugins {
    id "dev.kikugie.stonecutter" version "0.6-beta.3"
}

stonecutter {
    kotlinController = false
    centralScript = "build.gradle"
    
    // Subproject configuration
}
```
:::

## Specifying versions
Each Stonecutter version consists of two parts:
1. Subproject name, which will have a folder in `/versions/{project}` for per-version properties.
2. Minecraft version, which can be referenced in the buildscript and source code to modify the behaviour depending on it.

In the `stonecutter {}` extension you can specify, which projects should be versioned,
as well as register the versions.
::: tabs key:dsl
== settings.gradle.kts
```kotlin
stonecutter {
    create(rootProject) { // [!code ++:3]
        versions("1.20.1", "1.21.1", "1.21.4")
    }
}
```

== settings.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy {2-7}
stonecutter {
    kotlinController = false // [!code ++:6]
    centralScript = "build.gradle"
    
    create(getRootProject()) {
        versions "1.20.1", "1.21.1", "1.21.4"
    }
}
```
:::

## Version reset point
During mod development with Stonecutter, you're going to switch the active
version a lot, which modifies your source files, generating meaningless
git changes. Committing those can lead to a less readable version history.

To mitigate this issue, Stonecutter assigns a `VCS version` alongside
the `Reset active version` task. It's recommended to run it before committing your changes.

By default, it's assigned to the first registered version. For example, when we have
versions `1.20.1`, `1.21.1`, `1.21.4`, the VCS version will be `1.20.1`.
Alternatively, you can assign it explicitly:
::: tabs key:dsl
== settings.gradle.kts
```kotlin
stonecutter {
    create(rootProject) {
        versions("1.20.1", "1.21.1", "1.21.4")
        vcsVersion = "1.21.4" // [!code ++]
    }
}
```

== settings.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
stonecutter {
    kotlinController = false
    centralScript = "build.gradle"
    
    create(getRootProject()) {
        versions "1.20.1", "1.21.1", "1.21.4"
        vcsVersion = "1.21.4" // [!code ++]
    }
}
```
:::

## Applying changes
With projects configured, press the `Sync Gradle` button in IntelliJ,
which will create the `stonecutter.gradle[.kts]` file in the current directory.