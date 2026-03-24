package uk.gov.dluhc.printapi.config.aspect

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import java.util.concurrent.TimeUnit

@Component
class ScheduledJobAspectTestJob {
    @Scheduled(cron = "*/10 * * * * *")
    fun testFunction() {
        throwException()
    }

    fun throwException() {
        throw Exception("test exception scheduled aspect should catch me")
    }
}

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class ScheduledJobAspectIntegrationTest(
    @Value("\${alarm-magic-strings.scheduled-job}")
    val alarmString: String,
) : IntegrationTest() {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun scheduledJobProperties(registry: DynamicPropertyRegistry) {
            registry.add("jobs.enabled") { "true" }
        }
    }

    @Autowired
    protected lateinit var scheduledJobAspectTestJob: ScheduledJobAspectTestJob

    @Test
    fun `it should handle exceptions thrown by logging the alarm string`() {
        // When
        // The scheduled job will run every 10 seconds

        // Then
        await.pollDelay(2, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).untilAsserted {
            assertThat(TestLogAppender.hasLog("$alarmString [testFunction]", Level.ERROR)).isTrue
        }
    }

    @Test
    fun `it should rethrow the exception`() {
        // When, Then
        assertThrows<Exception> { scheduledJobAspectTestJob.testFunction() }
    }
}
