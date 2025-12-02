import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode

class STTestConfig : AbstractProjectConfig() {
    override val specExecutionMode: SpecExecutionMode = SpecExecutionMode.Concurrent
}