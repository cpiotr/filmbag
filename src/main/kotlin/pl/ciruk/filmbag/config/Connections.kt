package pl.ciruk.filmbag.config

import com.zaxxer.hikari.HikariDataSource
import org.mariadb.jdbc.MariaDbDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import redis.clients.jedis.JedisPoolConfig
import javax.sql.DataSource


@Configuration
class Connections {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ConditionalOnMissingClass("javax.sql.DataSource")
    @Bean
    fun dataSource(
            @Value("\${spring.datasource.url}") url: String,
            @Value("\${db.username}") username: String,
            @Value("\${db.password}") password: String): DataSource {
        logConfiguration("JDBC URL", url)

        val ds = HikariDataSource()
        ds.maximumPoolSize = 100
        ds.dataSourceClassName = MariaDbDataSource::class.java.name
        ds.addDataSourceProperty("url", url)
        ds.addDataSourceProperty("user", username)
        ds.addDataSourceProperty("password", password)
        return ds
    }

    @Bean
    fun redisConnectionFactory(
            @Value("\${redis.host}") redisHost: String,
            @Value("\${redis.pool.maxActive:8}") redisPoolMaxActive: Int): RedisConnectionFactory {
        logConfiguration("Redis host", redisHost)
        val redisStandaloneConfiguration = RedisStandaloneConfiguration(redisHost)

        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = redisPoolMaxActive
        poolConfig.maxWaitMillis = 1000
        poolConfig.minEvictableIdleTimeMillis = 100
        val jedisClientConfiguration = JedisClientConfiguration.builder()
                .usePooling()
                .poolConfig(poolConfig)
                .build()

        return JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<ByteArray, ByteArray> {
        val redisTemplate = RedisTemplate<ByteArray, ByteArray>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        redisTemplate.keySerializer = NoopSerializer()
        redisTemplate.valueSerializer = NoopSerializer()
        return redisTemplate
    }

    private fun logConfiguration(name: String, value: String) {
        logger.info("$name: <$value>")
    }

    class NoopSerializer : RedisSerializer<ByteArray> {
        override fun serialize(t: ByteArray?): ByteArray? {
            return t
        }

        override fun deserialize(bytes: ByteArray?): ByteArray? {
            return bytes
        }
    }
}
