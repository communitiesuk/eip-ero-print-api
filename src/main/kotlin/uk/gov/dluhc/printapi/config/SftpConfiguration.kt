package uk.gov.dluhc.printapi.config

import com.jcraft.jsch.ChannelSftp
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.expression.common.LiteralExpression
import org.springframework.integration.file.remote.session.CachingSessionFactory
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate

@Configuration
class SftpConfiguration {

    @Bean
    fun sftpRemoteFileTemplate(
        sessionFactory: SessionFactory<ChannelSftp.LsEntry>,
        properties: SftpProperties
    ): SftpRemoteFileTemplate {
        val template = SftpRemoteFileTemplate(sessionFactory)
        template.setRemoteDirectoryExpression(LiteralExpression(properties.printRequestUploadDirectory))
        template.temporaryFileSuffix = ".tmp"
        return template
    }

    @Bean
    fun sftpSessionFactory(properties: SftpProperties): SessionFactory<ChannelSftp.LsEntry> {
        val factory = DefaultSftpSessionFactory(true)
        factory.setHost(properties.host) // printer-a1-sftp-credentials.primarySftpServer
        factory.setPort(properties.port) // default port
        factory.setUser(properties.user) // printer-a1-sftp-credentials.sftpUsername
        factory.setPrivateKey(properties.privateKey) // printer-a1-sftp-credentials-private-ssh-key
        factory.setPrivateKeyPassphrase(properties.privateKeyPassphrase) // Nothing configured?
        factory.setPassword(properties.password) // printer-a1-sftp-credentials.sftpPassword
        factory.setAllowUnknownKeys(true)
        return CachingSessionFactory(factory)
    }
}

@ConfigurationProperties(prefix = "sftp")
@ConstructorBinding
data class SftpProperties(
    val host: String,
    val port: Int = 22,
    val user: String,
    val privateKey: Resource,
    val privateKeyPassphrase: String,
    val password: String,
    val printRequestUploadDirectory: String
)
