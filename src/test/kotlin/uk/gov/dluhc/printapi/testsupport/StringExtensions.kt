package uk.gov.dluhc.printapi.testsupport

fun String.replaceSpacesWith(replacement: String): String = replace(Regex("\\s+"), replacement)
