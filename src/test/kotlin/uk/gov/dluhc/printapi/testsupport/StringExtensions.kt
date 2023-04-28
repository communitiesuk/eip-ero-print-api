package uk.gov.dluhc.printapi.testsupport

fun String.replaceSpacesWith(replacement: String): String = replace(Regex("\\s+"), replacement)

fun buildSanitizedSurname(surname: String): String =
    surname
        .uppercase()
        .replace(Regex("-"), " ") // replace hyphen with space
        .replace(Regex("'"), "") // remove apostrophe
        .replace(Regex(" {2,}"), " ") // multiple spaces to single space
        .trim()
