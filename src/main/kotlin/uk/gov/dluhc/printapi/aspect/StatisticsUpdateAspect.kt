package uk.gov.dluhc.printapi.aspect

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate

/**
 * See the comment on `StatisticsUpdateEventListener` for why this Aspect indirectly triggers a statistics update via
 * the ApplicationEventPublisher rather than calling the StatisticsUpdateService directly.
 */
@Aspect
@Component
class StatisticsUpdateAspect(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(certificate)",
        returning = "saved"
    )
    fun afterSaveCertificate(joinPoint: JoinPoint, certificate: Certificate, saved: Certificate) {
        saved.sourceReference?.also {
            applicationEventPublisher.publishEvent(StatisticsUpdateEvent(it))
        }
    }

    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(temporaryCertificate)",
        returning = "saved"
    )
    fun afterSaveTemporaryCertificate(joinPoint: JoinPoint, temporaryCertificate: TemporaryCertificate, saved: TemporaryCertificate) {
        saved.sourceReference?.also {
            applicationEventPublisher.publishEvent(StatisticsUpdateEvent(it))
        }
    }

    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.saveAll(..))",
        returning = "saved"
    )
    fun afterSaveAll(joinPoint: JoinPoint, saved: Iterable<Any>) {
        if (saved.any { it is Certificate }) {
            (saved as Iterable<Certificate>).forEach {
                it.sourceReference?.also {
                    applicationEventPublisher.publishEvent(StatisticsUpdateEvent(it))
                }
            }
        }
        if (saved.any { it is TemporaryCertificate }) {
            (saved as Iterable<TemporaryCertificate>).forEach {
                it.sourceReference?.also {
                    applicationEventPublisher.publishEvent(StatisticsUpdateEvent(it))
                }
            }
        }
    }
}
