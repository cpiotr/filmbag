package pl.ciruk.filmbag.config

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
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
}

@JvmOverloads
fun String.asHttpGet(parameters: List<Pair<String, Any?>>? = listOf()): Request = this.httpGet(parameters)
        .timeout(1_000)
        .timeoutRead(30_000)
