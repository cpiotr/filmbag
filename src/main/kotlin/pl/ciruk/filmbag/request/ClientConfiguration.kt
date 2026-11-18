package pl.ciruk.filmbag.request

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.RedisClient
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class ClientConfiguration {
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

    @Bean
    fun httpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(1))
            .build()
    }
}
