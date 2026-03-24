package uk.gov.dluhc.printapi.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import uk.gov.dluhc.printapi.config.MetricsConfiguration

@ExtendWith(MockitoExtension::class)
internal class MetricsClientTest {
    private lateinit var metricsClient: MetricsClient

    @Mock
    private lateinit var cloudWatchClient: CloudWatchClient

    private val environment = "test-env-name"

    @Nested
    inner class MetricsEnabled {
        @BeforeEach
        fun setup() {
            val metricsConfiguration = MetricsConfiguration(environment, true)
            metricsClient = MetricsClient(cloudWatchClient, metricsConfiguration)
        }

        @Test
        fun `should record print requests sent`() {
            // When
            metricsClient.recordPrintRequestsSent(5)

            // Then
            val captor = argumentCaptor<PutMetricDataRequest>()
            verify(cloudWatchClient).putMetricData(captor.capture())
            val request = captor.firstValue

            assertThat(request.namespace()).isEqualTo("PrintApi")
            assertThat(request.metricData()).hasSize(1)
            with(request.metricData()[0]) {
                assertThat(metricName()).isEqualTo("PrintRequests-Sent")
                assertThat(unit()).isEqualTo(StandardUnit.COUNT)
                assertThat(value()).isEqualTo(5.0)
                assertThat(dimensions()).contains(
                    Dimension.builder().name("Environment").value(environment).build()
                )
            }
        }

        @Test
        fun `should record print response received`() {
            // When
            metricsClient.recordPrintResponseReceived()

            // Then
            val captor = argumentCaptor<PutMetricDataRequest>()
            verify(cloudWatchClient).putMetricData(captor.capture())
            val request = captor.firstValue

            assertThat(request.namespace()).isEqualTo("PrintApi")
            assertThat(request.metricData()).hasSize(1)
            with(request.metricData()[0]) {
                assertThat(metricName()).isEqualTo("PrintResponses-Received")
                assertThat(unit()).isEqualTo(StandardUnit.COUNT)
                assertThat(value()).isEqualTo(1.0)
                assertThat(dimensions()).contains(
                    Dimension.builder().name("Environment").value(environment).build()
                )
            }
        }
    }

    @Nested
    inner class MetricsDisabled {
        @BeforeEach
        fun setup() {
            val metricsConfiguration = MetricsConfiguration(environment, false)
            metricsClient = MetricsClient(cloudWatchClient, metricsConfiguration)
        }

        @Test
        fun `should not send metrics when recording print requests sent`() {
            // When
            metricsClient.recordPrintRequestsSent(3)

            // Then
            verifyNoInteractions(cloudWatchClient)
        }

        @Test
        fun `should not send metrics when recording print response received`() {
            // When
            metricsClient.recordPrintResponseReceived()

            // Then
            verifyNoInteractions(cloudWatchClient)
        }
    }
}
