package gradle

@DslMarker @Retention(AnnotationRetention.BINARY)
annotation class GradleTestDsl

const val WITH_DEBUG: String = "%debug%"
const val EXPECT_FAIL: String = "%fail%"
