package uk.gov.dluhc.printapi.database.repository

import java.util.UUID

abstract class PrintDetailsRepositoryException(message: String) : RuntimeException(message)

class PrintDetailsNotFoundException(id: UUID) :
    PrintDetailsRepositoryException("Print details not found for id: $id")
