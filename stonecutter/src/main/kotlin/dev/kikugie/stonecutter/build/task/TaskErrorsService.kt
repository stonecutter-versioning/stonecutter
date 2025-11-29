package dev.kikugie.stonecutter.build.task

import dev.kikugie.commons.then
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

internal abstract class TaskErrorsService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    abstract val errors: MapProperty<String, Boolean>

    override fun onFinish(event: FinishEvent?) {
    }

    fun isNew(message: String): Boolean = synchronized(errors) {
        if (errors.getting(message).isPresent) false
        else errors.put(message, true) then true
    }

    companion object {
        const val NAME: String = "StonecutterTaskErrors"
    }
}