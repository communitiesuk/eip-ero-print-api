package uk.gov.dluhc.printapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class ThreadPoolConfiguration {

    @Bean
    fun zipStreamProducerTaskExecutor(
        @Value("\${thread-pool.zip.core-size}") coreSize: Int,
        @Value("\${thread-pool.zip.max-size}") maxSize: Int,
    ): ThreadPoolTaskExecutor {
        val pool = ThreadPoolTaskExecutor()
        pool.corePoolSize = coreSize
        pool.maxPoolSize = maxSize
        pool.setWaitForTasksToCompleteOnShutdown(true)
        return pool
    }
}
