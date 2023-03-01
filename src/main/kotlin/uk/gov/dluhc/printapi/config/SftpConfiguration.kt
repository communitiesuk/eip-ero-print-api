package uk.gov.dluhc.printapi.config

import com.jcraft.jsch.ChannelSftp
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ByteArrayResource
import org.springframework.expression.common.LiteralExpression
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate

@Configuration
class SftpConfiguration {

    @Bean
    @Qualifier("sftpInboundTemplate")
    fun sftpInboundTemplate(
        sessionFactory: SessionFactory<ChannelSftp.LsEntry>,
        properties: SftpProperties
    ): SftpRemoteFileTemplate {
        val template = SftpRemoteFileTemplate(sessionFactory)
        template.setRemoteDirectoryExpression(LiteralExpression(properties.printRequestUploadDirectory))
        template.temporaryFileSuffix = ".tmp"
        return template
    }

    @Bean
    @Qualifier("sftpOutboundTemplate")
    fun sftpOutboundTemplate(
        sessionFactory: SessionFactory<ChannelSftp.LsEntry>,
        properties: SftpProperties
    ): SftpRemoteFileTemplate {
        val template = SftpRemoteFileTemplate(sessionFactory)
        template.setRemoteDirectoryExpression(LiteralExpression(properties.printResponseDownloadDirectory))
        return template
    }

    @Bean
    fun sftpSessionFactory(properties: SftpProperties): SessionFactory<ChannelSftp.LsEntry> =
        with(properties) {
            DefaultSftpSessionFactory(true).apply {
                setHost(host)
                setPort(port)
                setUser(user)
                setPassword(password)
                setPrivateKey(ByteArrayResource(privateKey.encodeToByteArray()))
                setAllowUnknownKeys(true)
            }
        }
}

@ConfigurationProperties(prefix = "sftp")
@ConstructorBinding
data class SftpProperties(
    val host: String,
    val port: Int = 22,
    val user: String,
    val password: String,
    val privateKey: String,
    val printRequestUploadDirectory: String,
    val printResponseDownloadDirectory: String
)
