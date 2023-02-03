package uk.gov.dluhc.printapi.rest

const val HAS_ERO_VC_ADMIN_AUTHORITY = """
    hasAnyAuthority("ero-vc-admin-".concat(#eroId))
"""
