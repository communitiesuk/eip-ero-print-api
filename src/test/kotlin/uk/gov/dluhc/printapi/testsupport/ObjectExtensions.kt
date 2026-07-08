package uk.gov.dluhc.printapi.testsupport

import tools.jackson.databind.json.JsonMapper

private val jsonMapper = JsonMapper()

fun <T> T.deepCopy(): T {
    return jsonMapper.readValue(jsonMapper.writeValueAsString(this), this!!::class.java)
}
