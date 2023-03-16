package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.service.AedDataRetentionService
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService
import uk.gov.dluhc.printapi.service.TemporaryCertificateDataRetentionService

@Component
class FinalRetentionPeriodDataRemovalJob(
    private val certificateDataRetentionService: CertificateDataRetentionService,
    private val temporaryCertificateDataRetentionService: TemporaryCertificateDataRetentionService,
    private val aedDataRetentionService: AedDataRetentionService
) {

    @Scheduled(cron = "\${jobs.remove-vca-final-retention-period-data.cron}")
    @SchedulerLock(name = "\${jobs.remove-vca-final-retention-period-data.name}")
    fun removeVoterCardFinalRetentionPeriodData() {
        temporaryCertificateDataRetentionService.removeTemporaryCertificateData(sourceType = VOTER_CARD)
        certificateDataRetentionService.queueCertificatesForRemoval(sourceType = VOTER_CARD)
    }

    @Scheduled(cron = "\${jobs.remove-aed-final-retention-period-data.cron}")
    @SchedulerLock(name = "\${jobs.remove-aed-final-retention-period-data.name}")
    fun removeAedFinalRetentionPeriodData() {
        aedDataRetentionService.removeAnonymousElectorDocumentData(sourceType = ANONYMOUS_ELECTOR_DOCUMENT)
    }
}
