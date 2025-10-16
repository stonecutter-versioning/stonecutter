package gradle

import io.kotest.engine.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.net.URI

object GradleVersions {
    private val client = newClient()

    val ALL: Set<GradleDistribution> by lazy {
        buildSet {
            this += VGradleDistribution("8.14.3")
            this += VGradleDistribution(GradleVersion.current().version)
            client.getGradleDistribution("https://services.gradle.org/versions/release-candidate")
                ?.let { release -> DGradleDistribution(release.version, URI.create(release.downloadUrl)) }
                ?.let { this += it }
        }
    }

    sealed interface GradleDistribution {
        val version: String
        fun apply(runner: GradleRunner)
    }

    private data class VGradleDistribution(override val version: String) : GradleDistribution {
        override fun apply(runner: GradleRunner) {
            runner.withGradleVersion(version)
        }
    }

    private data class DGradleDistribution(override val version: String, val uri: URI) : GradleDistribution {
        override fun apply(runner: GradleRunner) {
            runner.withGradleDistribution(uri)
        }
    }
}

@Serializable
private data class GradleRelease(val downloadUrl: String, val version: String, val broken: Boolean)

private fun newClient(): HttpClient = HttpClient(Java) {
    install(UserAgent) {
        agent = "kikugie/stonecutter"
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}

private fun HttpClient.getGradleDistribution(url: String) = runBlocking {
    runCatching { get(url).body<GradleRelease>() }.getOrNull()?.takeUnless(GradleRelease::broken)
}