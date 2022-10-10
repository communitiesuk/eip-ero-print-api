package uk.gov.dluhc.printapi.testsupport.testdata

import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun aValidRandomEroId() = "${faker.address().city().lowercase()}-city-council"
