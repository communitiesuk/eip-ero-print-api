package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateApplicationStatisticsMessage
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateStatisticsMessage

class StatisticsUpdateServiceTest {

    private val triggerVoterCardStatisticsUpdateQueue: MessageQueue<UpdateStatisticsMessage> = mock()
    private val triggerApplicationStatisticsUpdateQueue: MessageQueue<UpdateApplicationStatisticsMessage> = mock()

    private val statisticsUpdateService = StatisticsUpdateService(
        triggerVoterCardStatisticsUpdateQueue,
        triggerApplicationStatisticsUpdateQueue
    )

    @Test
    fun `should trigger application statistics update`() {
        val id = aValidSourceReference()

        statisticsUpdateService.updateStatistics(id)

        verify(triggerApplicationStatisticsUpdateQueue).submit(
            argThat { applicationId == id },
            any()
        )
    }

    @Test
    fun `should generate unique message-deduplication-id`() {
        val applicationId = aValidSourceReference()

        statisticsUpdateService.updateStatistics(applicationId)

        val argumentCaptor = argumentCaptor<Map<String, String>>()
        verify(triggerApplicationStatisticsUpdateQueue).submit(any(), argumentCaptor.capture())

        val headers = argumentCaptor.firstValue
        val deduplicationId = headers["message-deduplication-id"]

        assertNotNull(deduplicationId)
        assertEquals(36, deduplicationId?.length) // UUID length
    }

    @Test
    fun `should trigger voter card statistics update with correct parameters`() {
        val applicationId = aValidSourceReference()

        statisticsUpdateService.triggerVoterCardStatisticsUpdate(applicationId)

        verify(triggerVoterCardStatisticsUpdateQueue).submit(
            argThat { voterCardApplicationId == applicationId },
            argThat { headers ->
                headers["message-group-id"] == applicationId && headers.containsKey("message-deduplication-id") // Checking headers
            }
        )
    }

    @Test
    fun `should trigger application statistics update with correct parameters`() {
        val id = aValidSourceReference()

        statisticsUpdateService.triggerApplicationStatisticsUpdate(id)

        verify(triggerApplicationStatisticsUpdateQueue).submit(
            argThat { applicationId == id },
            argThat { headers ->
                headers["message-group-id"] == id && headers.containsKey("message-deduplication-id") // Checking headers
            }
        )
    }
}
