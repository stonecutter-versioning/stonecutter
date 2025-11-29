package dev.kikugie.stonecutter

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension
import dev.kikugie.stonecutter.settings.StonecutterSettingsImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.create
import javax.inject.Inject

public abstract class StonecutterPlugin @Inject constructor(
    private val objects: ObjectFactory,
    private val registry: BuildEventsListenerRegistry
) : Plugin<ExtensionAware> {
    public companion object {
        /**Current Stonecutter version.*/ // Updated by ':updateVersion' task during build
        public const val VERSION: String = "0.8-alpha.14"
    }

    /**
     * Applies the plugin either to [Settings] or [Project].
     * Applying the plugin to an incorrect target will throw an exception.
     */
    @OptIn(StonecutterInternalAPI::class)
    override fun apply(target: ExtensionAware): Unit = when (target) {
        is Settings -> {
            target.stonecutter<StonecutterSettingsExtension, StonecutterSettingsImpl>(registry)
        }

        is Project ->
            if (target.getController() == null) target.stonecutter<StonecutterBuildExtension, StonecutterBuildImpl>()
            else target.stonecutter<StonecutterControllerExtension, StonecutterControllerImpl>()

        else ->
            error("The plugin may only be applied to settings and projects")
    }

    private inline fun <reified P : Any, reified R : P> ExtensionAware.stonecutter(vararg args: Any) {
        val it = extensions.create(P::class, "stonecutter", R::class, this, *args)
        if (!extensions.extraProperties.has("dev.kikugie.stonecutter.no_short_extension"))
            extensions.add(P::class, "sc", it)
    }
}