package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.service.AedDataRetentionService
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService

@Component
class InitialRetentionPeriodDataRemovalJob(
    private val certificateDataRetentionService: CertificateDataRetentionService,
    private val aedDataRetentionService: AedDataRetentionService
) {

    @Scheduled(cron = "\${jobs.remove-vca-initial-retention-period-data.cron}")
    @SchedulerLock(name = "\${jobs.remove-vca-initial-retention-period-data.name}")
    fun removeVoterCardInitialRetentionPeriodData() {
        certificateDataRetentionService.removeInitialRetentionPeriodData(sourceType = VOTER_CARD)
    }

    @Scheduled(cron = "\${jobs.remove-aed-initial-retention-period-data.cron}")
    @SchedulerLock(name = "\${jobs.remove-aed-initial-retention-period-data.name}")
    fun removeAedInitialRetentionPeriodData() {
        aedDataRetentionService.removeInitialRetentionPeriodData(sourceType = ANONYMOUS_ELECTOR_DOCUMENT)
    }
}
