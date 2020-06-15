package pl.ciruk.filmbag.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.testFilmRequest

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
class KMariaDbContainer : MariaDBContainer<KMariaDbContainer>()

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = [FilmBagApplication::class])
@Testcontainers
class JournalTest(@Autowired val journal: Journal) {

    @Test
    fun `should record request list to journal and replay it`() {
        val expectedRequests = mutableListOf(testFilmRequest())

        journal.record(expectedRequests)
        val replayedRequests = journal.replay().toList()

        assertThat(replayedRequests)
                .containsExactly(expectedRequests)
    }

    @Test
    fun `should replay empty journal`() {
        val replayedRequests = journal.replay().toList()

        assertThat(replayedRequests)
                .isEmpty()
    }

    companion object {
        @Container
        val redis = KGenericContainer("redis:5.0.3-alpine")
                .withExposedPorts(6379)!!

        @Container
        val mariaDb = KMariaDbContainer()
                .withUsername("root")
                .withPassword("")!!

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("redis.host") { redis.containerIpAddress }
            registry.add("redis.port") { redis.firstMappedPort }
            registry.add("spring.datasource.url") { mariaDb.jdbcUrl }
        }
    }
}
