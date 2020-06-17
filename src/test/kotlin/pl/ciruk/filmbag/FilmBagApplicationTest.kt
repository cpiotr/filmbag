package pl.ciruk.filmbag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.ciruk.filmbag.integration.KMariaDbContainer
import pl.ciruk.filmbag.request.DataLoader

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Testcontainers
class FilmBagApplicationTest(@Autowired val context: ApplicationContext) {
    @Test
    fun shouldLoadContext() {
        val dataLoader = context.getBean(DataLoader::class.java)
        assertThat(dataLoader).isNotNull
    }

    companion object {
        @Container
        val mariaDb = KMariaDbContainer()
                .withUsername("root")
                .withPassword("")!!

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mariaDb.jdbcUrl }
        }
    }
}
