package uk.gov.dluhc.printapi.testsupport

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

/**
 * Generates an RSA private and public key, and jwk (json web key) to be used by tests.
 */
object RsaKeyPair {
    private val keyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    val publicKey = keyPair.public as RSAPublicKey
    val privateKey = keyPair.private as RSAPrivateKey
    val jwk: JWK = RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .build()
}
