package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService

@Component
class InitialRetentionPeriodDataRemovalJob(
    private val certificateDataRetentionService: CertificateDataRetentionService
) {

    @Scheduled(cron = "\${jobs.remove-vca-initial-retention-period-data.cron}")
    @SchedulerLock(name = "\${jobs.remove-vca-initial-retention-period-data.name}")
    fun removeVoterCardInitialRetentionPeriodData() {
        certificateDataRetentionService.removeInitialRetentionPeriodData(sourceType = VOTER_CARD)
    }
}
