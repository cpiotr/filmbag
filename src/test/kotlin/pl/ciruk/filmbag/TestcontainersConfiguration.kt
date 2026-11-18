package pl.ciruk.filmbag

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.mariadb.MariaDBContainer

/** MariaDB and Redis in containers, at the versions `docker-compose.yml` runs. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun mariaDbContainer() = MariaDBContainer("mariadb:11.4")

    @Bean
    fun redisContainer() = RedisContainer()

    @Bean
    fun redisProperties(redis: RedisContainer) = DynamicPropertyRegistrar { registry ->
        registry.add("redis.host") { redis.host }
        registry.add("redis.port") { redis.getMappedPort(RedisContainer.PORT) }
    }
}

class RedisContainer : GenericContainer<RedisContainer>("redis:8.4-alpine") {
    init {
        withExposedPorts(PORT)
    }

    companion object {
        const val PORT = 6379
    }
}
