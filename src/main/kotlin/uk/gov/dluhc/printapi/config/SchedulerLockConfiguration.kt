package uk.gov.dluhc.printapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.dynamodb2.DynamoDBLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@Configuration
@EnableScheduling
@ConditionalOnProperty("jobs.enabled", havingValue = "true")
@EnableSchedulerLock(defaultLockAtMostFor = "\${jobs.lock-at-most-for}")
class SchedulerLockConfiguration(
    private val dynamoDbClient: DynamoDbClient,
    private val dynamoDbConfiguration: DynamoDbConfiguration
) {

    @Bean
    fun schedulerLockProvider(): LockProvider {
        return DynamoDBLockProvider(dynamoDbClient, dynamoDbConfiguration.schedulerLockTableName)
    }
}
