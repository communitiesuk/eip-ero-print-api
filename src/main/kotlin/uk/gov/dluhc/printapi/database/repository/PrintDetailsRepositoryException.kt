package uk.gov.dluhc.printapi.database.repository

import java.util.UUID

abstract class PrintDetailsRepositoryException(message: String) : RuntimeException(message)

class PrintDetailsNotFoundException : PrintDetailsRepositoryException {
    constructor(id: UUID) : super("Print details not found for id: $id")
    constructor(requestId: String) : super("Print details not found for requestId: $requestId")
}
