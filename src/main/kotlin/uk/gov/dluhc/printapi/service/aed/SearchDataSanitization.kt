package uk.gov.dluhc.printapi.service.aed

import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.StringUtils.SPACE

fun sanitizeSurname(surname: String): String =
    surname
        .uppercase()
        .replace(Regex("-"), SPACE) // replace hyphen with space
        .replace(Regex("'"), EMPTY) // remove apostrophe
        .replace(Regex(" {2,}"), SPACE) // multiple spaces to single space
        .trim()

fun sanitizeApplicationReference(applicationReference: String): String =
    applicationReference
        .uppercase()
        .replace(Regex(" +"), EMPTY) // any number of spaces to empty
