package uk.gov.dluhc.printapi.service.temporarycertificate

data class ImageDetails(
    val pageNumber: Int = 1,
    val absoluteX: Float,
    val absoluteY: Float,
    val fitWidth: Float,
    val fitHeight: Float,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDetails

        if (pageNumber != other.pageNumber) return false
        if (absoluteX != other.absoluteX) return false
        if (absoluteY != other.absoluteY) return false
        if (fitWidth != other.fitWidth) return false
        if (fitHeight != other.fitHeight) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageNumber
        result = 31 * result + absoluteX.hashCode()
        result = 31 * result + absoluteY.hashCode()
        result = 31 * result + fitWidth.hashCode()
        result = 31 * result + fitHeight.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
