package dev.kikugie.stonecutter

import dev.kikugie.stonecutter.build.StonecutterBuild
import dev.kikugie.stonecutter.controller.StonecutterController
import dev.kikugie.stonecutter.controller.manager.controller
import dev.kikugie.stonecutter.data.container.ConfigurationService
import dev.kikugie.stonecutter.settings.StonecutterSettings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider

public open class StonecutterPlugin : Plugin<ExtensionAware> {
    public companion object {
        /**Current Stonecutter version.*/ // Updated by ':updateVersion' task during build
        @StonecutterAPI
        public const val VERSION: String = "0.6.1-beta.1"

        internal lateinit var SERVICE: Provider<ConfigurationService>

        private fun Gradle.createConfigurationService() = sharedServices
            .registerIfAbsent(ConfigurationService.Companion.NAME, ConfigurationService::class.java)
            .also { SERVICE = it }
    }

    override fun apply(target: ExtensionAware) {
        if (target is Settings) target.gradle.createConfigurationService()
        val type = if (target is Settings)
            StonecutterSettings::class
        else if (target is Project)
            if (target.controller() == null) StonecutterBuild::class
            else StonecutterController::class
        else error("The plugin may only be applied to settings and projects")
        target.extensions.create("stonecutter", type.java, target)
    }
}