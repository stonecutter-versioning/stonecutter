package dev.kikugie.stonecutter.util

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.build.ext.ConstantContainer
import dev.kikugie.stonecutter.build.ext.DependencyContainer
import dev.kikugie.stonecutter.build.ext.ReplacementContainer
import dev.kikugie.stonecutter.build.ext.SwapContainer
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.ext.FlagContainer
import dev.kikugie.stonecutter.controller.ext.MutableFlagContainer
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasks
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasksImpl
import org.gradle.kotlin.dsl.the

public val StonecutterBuildExtension.constants: ConstantContainer
    get() = the<ConstantContainer>()

public val StonecutterBuildExtension.swaps: SwapContainer
    get() = the<SwapContainer>()

public val StonecutterBuildExtension.dependencies: DependencyContainer
    get() = the<DependencyContainer>()

public val StonecutterBuildExtension.replacements: ReplacementContainer
    get() = the<ReplacementContainer>()

public val StonecutterBuildExtension.flags: FlagContainer
    get() = the<FlagContainer>()

public val StonecutterBuildExtension.tasks: StonecutterBuildTasks
    get() = the<StonecutterBuildTasks>()

internal val StonecutterBuildImpl.tasks: StonecutterBuildTasksImpl
    get() = the<StonecutterBuildTasks>() as StonecutterBuildTasksImpl

public val StonecutterControllerExtension.flags: MutableFlagContainer
    get() = the<MutableFlagContainer>()

public val StonecutterControllerExtension.tasks: StonecutterControllerTasks
    get() = the<StonecutterControllerTasks>()

internal val StonecutterControllerImpl.tasks: StonecutterControllerTasksImpl
    get() = the<StonecutterControllerTasks>() as StonecutterControllerTasksImpl