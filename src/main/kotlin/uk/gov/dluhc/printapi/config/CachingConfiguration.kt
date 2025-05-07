package uk.gov.dluhc.printapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

const val UK_BANK_HOLIDAYS_CACHE = "ukBankHolidaysData"

@Configuration
@EnableCaching
class CachingConfiguration {
    @Value("\${caching.time-to-live}")
    private lateinit var timeToLive: Duration

    @Bean
    fun cacheManager(): CacheManager {
        return CaffeineCacheManager()
            .apply {
                setCaffeine(Caffeine.newBuilder().expireAfterWrite(timeToLive))
                setCacheNames(listOf(UK_BANK_HOLIDAYS_CACHE))
            }
    }
}
