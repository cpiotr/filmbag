package pl.ciruk.filmbag.config

import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.RedisClient
import java.time.Duration
import jakarta.annotation.PostConstruct

@Configuration
class Connections(@Value("\${spring.datasource.url}") private val url: String) {
    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun logConnection() {
        logger.logConfiguration("JDBC URL", url)
    }

    @Bean
    fun redisClient(
            @Value("\${redis.host}") redisHost: String,
            @Value("\${redis.port}") redisPort: Int,
            @Value("\${redis.pool.maxActive:8}") redisPoolMaxActive: Int): RedisClient {
        logger.logConfiguration("Redis URL", "$redisHost:$redisPort")

        val poolConfig = ConnectionPoolConfig()
        poolConfig.maxTotal = redisPoolMaxActive
        poolConfig.setMaxWait(Duration.ofMillis(1000))
        poolConfig.minEvictableIdleDuration = Duration.ofMillis(100)
        return RedisClient.builder()
            .poolConfig(poolConfig)
            .hostAndPort(redisHost, redisPort)
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
