package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ClassPathResource
import software.amazon.awssdk.utils.IoUtils
import java.net.URI

/**
 * Class to bind configuration properties for the email content
 */
@ConfigurationProperties(prefix = "email.content")
class EmailContentConfiguration(
    portalBaseUrl: URI,
    vacContextRoot: String,
    aedContextRoot: String,
    val certificateReturned: EmailContentProperties,
    val certificateFailedToPrint: EmailContentProperties,
) {
    val vacBaseUrl: String
    val aedBaseUrl: String

    init {
        val portalBaseUrlWithoutTrailingSlash = getPortalBaseUrlWithoutTrailingSlash(portalBaseUrl)
        vacBaseUrl = portalBaseUrlWithoutTrailingSlash + "/" + vacContextRoot.trim('/')
        aedBaseUrl = portalBaseUrlWithoutTrailingSlash + "/" + aedContextRoot.trim('/')
    }

    private fun getPortalBaseUrlWithoutTrailingSlash(portalBaseUrl: URI) = portalBaseUrl.toASCIIString().trimEnd('/')
}

open class EmailContentProperties(
    val sendToRequestingUser: Boolean,
    val subject: String,
    emailBodyTemplate: String,
) {
    val emailBody: String

    init {
        with(ClassPathResource(emailBodyTemplate)) {
            emailBody = inputStream.use {
                IoUtils.toUtf8String(it)
            }
        }
    }
}
