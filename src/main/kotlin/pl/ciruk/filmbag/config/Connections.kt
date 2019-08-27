package pl.ciruk.filmbag.config

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.mariadb.jdbc.MariaDbDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import javax.sql.DataSource

@Configuration
class Connections {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun dataSource(
            @Value("\${spring.datasource.url}") url: String,
            @Value("\${db.username}") username: String,
            @Value("\${db.password}") password: String): DataSource {
        logConfiguration("JDBC URL", url)
        val ds = HikariDataSource()
        ds.maximumPoolSize = 10
        ds.dataSourceClassName = MariaDbDataSource::class.java.name
        ds.addDataSourceProperty("url", url)
        ds.addDataSourceProperty("user", username)
        ds.addDataSourceProperty("password", password)
        return ds
    }

    @Bean
    fun redisConnectionPool(
            @Value("\${redis.host}") redisHost: String,
            @Value("\${redis.port}") redisPort: Int,
            @Value("\${redis.pool.maxActive:8}") redisPoolMaxActive: Int): JedisPool {
        logConfiguration("Redis URL", "$redisHost:$redisPort")

        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = redisPoolMaxActive
        poolConfig.maxWaitMillis = 1000
        poolConfig.minEvictableIdleTimeMillis = 100
        return JedisPool(poolConfig, redisHost, redisPort)
    }

    private fun logConfiguration(name: String, value: String) {
        logger.info { "$name: <$value>" }
    }
}

@JvmOverloads
fun String.asHttpGet(parameters: List<Pair<String, Any?>>? = listOf()): Request = this.httpGet(parameters)
        .timeout(1_000)
        .timeoutRead(30_000)
