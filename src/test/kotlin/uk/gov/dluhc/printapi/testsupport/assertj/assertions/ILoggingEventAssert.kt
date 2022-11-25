package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import ch.qos.logback.classic.spi.ILoggingEvent
import org.apache.commons.lang3.StringUtils.isBlank
import org.assertj.core.api.AbstractAssert
import uk.gov.dluhc.printapi.config.CORRELATION_ID

class ILoggingEventAssert(actual: ILoggingEvent?) :
    AbstractAssert<ILoggingEventAssert, ILoggingEvent?>(actual, ILoggingEventAssert::class.java) {

    companion object {
        fun assertThat(actual: ILoggingEvent?) = ILoggingEventAssert(actual)
    }

    fun hasCorrelationId(expected: String?): ILoggingEventAssert {
        isNotNull
        with(actual!!) {
            if (mdcPropertyMap[CORRELATION_ID] != expected) {
                failWithMessage("Expected correlation ID to be $expected, but was $mdcPropertyMap[CORRELATION_ID]")
            }
        }
        return this
    }

    fun hasAnyCorrelationId(): ILoggingEventAssert {
        isNotNull
        with(actual!!) {
            if (isBlank(mdcPropertyMap[CORRELATION_ID])) {
                failWithMessage("Expected log message to have a correlation ID, but it did not")
            }
        }
        return this
    }
}
