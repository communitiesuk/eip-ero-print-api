package uk.gov.dluhc.printapi.service

import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus

fun countPrintRequestsAssignedToBatch(certificates: List<Certificate>, batchId: String): Int =
    certificates
        .flatMap { it.printRequests }
        .filter { it.batchId == batchId }
        .count { it.getCurrentStatus().status == PrintRequestStatus.Status.ASSIGNED_TO_BATCH }
