package uk.gov.dluhc.printapi.client

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import uk.gov.dluhc.printapi.config.MetricsConfiguration
import java.time.Instant

@Component
class MetricsClient(
    private val cloudWatchClient: CloudWatchClient,
    private val metricsConfiguration: MetricsConfiguration,
) {
    companion object {
        private const val NAMESPACE = "PrintApi"
        private const val COUNT_UNIT = "Count"
        private const val PRINT_REQUESTS_SENT = "PrintRequests-Sent"
        private const val PRINT_RESPONSES_RECEIVED = "PrintResponses-Received"
    }

    fun recordPrintRequestsSent(count: Int) {
        incrementCount(PRINT_REQUESTS_SENT, count.toDouble())
    }

    fun recordPrintResponseReceived() {
        incrementCount(PRINT_RESPONSES_RECEIVED, 1.0)
    }

    private fun incrementCount(metricName: String, value: Double) {
        val metric = MetricDatum.builder()
            .metricName(metricName)
            .dimensions(getDimension())
            .unit(COUNT_UNIT)
            .value(value)
            .timestamp(Instant.now())
            .build()

        val request = PutMetricDataRequest.builder()
            .namespace(NAMESPACE)
            .metricData(metric)
            .build()

        if (metricsConfiguration.metricsEnabled) {
            cloudWatchClient.putMetricData(request)
        }
    }

    private fun getDimension(): Dimension =
        Dimension.builder()
            .name("Environment")
            .value(metricsConfiguration.environment)
            .build()
}
