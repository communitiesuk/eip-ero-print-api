package uk.gov.dluhc.printapi.service

private const val WALES_GSS_CODE_NATION_LETTER = 'W'

fun isWalesCode(gssCode: String): Boolean {
    return gssCode.first() == WALES_GSS_CODE_NATION_LETTER
}
