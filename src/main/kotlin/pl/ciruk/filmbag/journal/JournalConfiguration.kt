package pl.ciruk.filmbag.journal

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.RedisClient
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class JournalConfiguration {
    @Bean
    fun redisClient(journal: JournalProperties): RedisClient {
        val poolConfig = ConnectionPoolConfig()
        poolConfig.maxTotal = journal.maxConnections
        poolConfig.setMaxWait(Duration.ofSeconds(1))
        return RedisClient.builder()
            .poolConfig(poolConfig)
            .hostAndPort(journal.host, journal.port)
            .build()
    }
}
