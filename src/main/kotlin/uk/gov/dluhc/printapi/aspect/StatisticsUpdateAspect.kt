package uk.gov.dluhc.printapi.aspect

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.service.StatisticsUpdateService

@Aspect
@Component
class StatisticsUpdateAspect(
    private val statisticsUpdateService: StatisticsUpdateService
) {
    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(certificate)",
        returning = "saved"
    )
    fun afterSaveCertificate(joinPoint: JoinPoint, certificate: Certificate, saved: Certificate) {
        saved.sourceReference?.also {
            statisticsUpdateService.triggerVoterCardStatisticsUpdate(it)
        }
    }

    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(temporaryCertificate)",
        returning = "saved"
    )
    fun afterSaveTemporaryCertificate(joinPoint: JoinPoint, temporaryCertificate: TemporaryCertificate, saved: TemporaryCertificate) {
        saved.sourceReference?.also {
            statisticsUpdateService.triggerVoterCardStatisticsUpdate(it)
        }
    }

    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(anonymousElectorDocument)",
        returning = "saved"
    )
    fun afterSaveAnonymousElectorDocument(joinPoint: JoinPoint, anonymousElectorDocument: AnonymousElectorDocument, saved: AnonymousElectorDocument) {
        statisticsUpdateService.triggerVoterCardStatisticsUpdate(saved.sourceReference)
    }

    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.saveAll(..))",
        returning = "saved"
    )
    fun afterSaveAll(joinPoint: JoinPoint, saved: Iterable<Any>) {
        if (saved.any { it is Certificate }) {
            (saved as Iterable<Certificate>).forEach {
                it.sourceReference?.also {
                    statisticsUpdateService.triggerVoterCardStatisticsUpdate(it)
                }
            }
        }
        if (saved.any { it is TemporaryCertificate }) {
            (saved as Iterable<TemporaryCertificate>).forEach {
                it.sourceReference?.also {
                    statisticsUpdateService.triggerVoterCardStatisticsUpdate(it)
                }
            }
        }
        if (saved.any { it is AnonymousElectorDocument }) {
            (saved as Iterable<AnonymousElectorDocument>).forEach {
                statisticsUpdateService.triggerVoterCardStatisticsUpdate(it.sourceReference)
            }
        }
    }
}
