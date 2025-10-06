package uk.gov.dluhc.printapi.testsupport.testdata

import io.jsonwebtoken.Jwts
import uk.gov.dluhc.eromanagementapi.models.EroGroup
import uk.gov.dluhc.printapi.config.IntegrationTest.Companion.ERO_ID
import uk.gov.dluhc.printapi.testsupport.RsaKeyPair
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

const val UNAUTHORIZED_BEARER_TOKEN: String =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQHdpbHRzaGlyZS5nb3YudWsiLCJpYXQiOjE1MTYyMzkwMjIsImF1dGhvcml0aWVzIjpbImVyby1hZG1pbiJdfQ.-pxW8z2xb-AzNLTRP_YRnm9fQDcK6CLt6HimtS8VcDY"

fun getBearerToken(
    eroId: String = aValidRandomEroId(),
    email: String = "an-ero-user@$eroId.gov.uk",
    groups: List<String> = listOf("ero-$eroId", "ero-vc-admin-$eroId")
): String =
    "Bearer ${buildAccessToken(eroId, email, groups)}"

fun getVCAdminBearerToken(eroId: String = ERO_ID, userName: String = "an-ero-user2@$eroId.gov.uk") =
    getBearerToken(groups = listOf("ero-$eroId", "ero-vc-admin-$eroId"), email = userName)

fun getVCAnonymousAdminBearerToken(eroId: String = ERO_ID, userName: String = "an-ero-user1@$eroId.gov.uk") =
    getBearerToken(groups = listOf("ero-$eroId", "ero-vc-anonymous-admin-$eroId"), email = userName)

fun getBearerTokenWithAllRolesExcept(
    eroId: String = aValidRandomEroId(),
    email: String = "an-ero-user3@$eroId.gov.uk",
    excludedRoles: List<String> = listOf("ero-vc-admin")
): String {
    val excludedGroups = excludedRoles.map { "$it-$eroId" }.toSet()
    val allGroupEroNames = EroGroup.values().map { it.value }.map { "ero-$it-$eroId" }.toSet()
    val requiredGroups = allGroupEroNames - excludedGroups
    return getBearerToken(eroId, email, requiredGroups.toList())
}

fun buildAccessToken(
    eroId: String = aValidRandomEroId(),
    email: String = "an-ero-user@$eroId.gov.uk",
    groups: List<String> = listOf("ero-$eroId", "ero-vc-admin-$eroId")
): String =
    Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claims(
            mapOf(
                "cognito:groups" to groups,
                "email" to email
            )
        )
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .signWith(RsaKeyPair.privateKey, Jwts.SIG.RS256)
        .compact()
