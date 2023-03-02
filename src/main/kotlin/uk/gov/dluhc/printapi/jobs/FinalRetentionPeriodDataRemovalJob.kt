package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService

@Component
class FinalRetentionPeriodDataRemovalJob(
    private val certificateDataRetentionService: CertificateDataRetentionService
) {

    @Scheduled(cron = "\${jobs.remove-vca-final-retention-period-data.cron}")
    @SchedulerLock(name = "\${jobs.remove-vca-final-retention-period-data.name}")
    fun removeVoterCardFinalRetentionPeriodData() {
        certificateDataRetentionService.removeFinalRetentionPeriodData(sourceType = VOTER_CARD)
    }
}
