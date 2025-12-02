import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode

class SCTestConfig : AbstractProjectConfig() {
    override val specExecutionMode: SpecExecutionMode = SpecExecutionMode.Concurrent
}