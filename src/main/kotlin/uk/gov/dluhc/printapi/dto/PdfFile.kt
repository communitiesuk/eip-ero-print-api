package uk.gov.dluhc.printapi.dto

data class PdfFile(
    val filename: String,
    val contents: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PdfFile

        if (filename != other.filename) return false
        if (!contents.contentEquals(other.contents)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + contents.contentHashCode()
        return result
    }
}
