package pl.ciruk.filmbag.config

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.time.Duration
import javax.annotation.PostConstruct

@Configuration
class Connections(@Value("\${spring.datasource.url}") private val url: String) {
    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun logConnection() {
        logger.logConfiguration("JDBC URL", url)
    }

    @Bean
    fun redisConnectionPool(
            @Value("\${redis.host}") redisHost: String,
            @Value("\${redis.port}") redisPort: Int,
            @Value("\${redis.pool.maxActive:8}") redisPoolMaxActive: Int): JedisPool {
        logger.logConfiguration("Redis URL", "$redisHost:$redisPort")

        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = redisPoolMaxActive
        poolConfig.maxWaitMillis = 1000
        poolConfig.minEvictableIdleTimeMillis = 100
        return JedisPool(poolConfig, redisHost, redisPort)
    }

    @Bean
    fun httpClient(objectMapper: ObjectMapper): OkHttpClient {
        return OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(1))
                .build()
    }
}
