package pl.ciruk.filmbag.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.identityHashOf
import pl.ciruk.filmbag.integration.KMariaDbContainer
import pl.ciruk.filmbag.integration.TestConfiguration
import pl.ciruk.filmbag.testOtherFilmRequest
import java.sql.DriverManager

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [FilmBagApplication::class, TestConfiguration::class]
)
@Testcontainers
class RequestProcessorRetryTest(@Autowired val requestProcessor: RequestProcessor) {
    @MockitoSpyBean
    lateinit var filmService: FilmService

    @Test
    fun `should update a film that another request stored after this one looked for it`() {
        val filmRequest = testOtherFilmRequest()
        val identityHash = identityHashOf(filmRequest.title!!, filmRequest.year!!, filmRequest.genres)
        doAnswer {
            // Runs after this request has read the database, so its first attempt inserts a duplicate
            insertFilmFromAnotherConnection(identityHash)
            it.callRealMethod()
        }.doCallRealMethod().`when`(filmService).storeAll(anyList())

        requestProcessor.storeAll(listOf(filmRequest))

        assertThat(query("SELECT id FROM film WHERE identity_hash = '$identityHash'")).containsExactly(STORED_FILM_ID)
        assertThat(query("SELECT film_id FROM score")).containsOnly(STORED_FILM_ID).hasSize(filmRequest.scores.size)
    }

    private fun insertFilmFromAnotherConnection(identityHash: String) {
        DriverManager.getConnection(mariaDb.jdbcUrl, mariaDb.username, mariaDb.password).use { connection ->
            connection.createStatement().use {
                it.executeUpdate(
                    "INSERT INTO film (id, title, year, link, score, identity_hash) " +
                        "VALUES ($STORED_FILM_ID, 'Stored by another request', 1999, 'link', 0.5, '$identityHash')"
                )
            }
        }
    }

    private fun query(sql: String): List<Long> =
        DriverManager.getConnection(mariaDb.jdbcUrl, mariaDb.username, mariaDb.password).use { connection ->
            connection.createStatement().use { statement ->
                val rows = statement.executeQuery(sql)
                generateSequence { if (rows.next()) rows.getLong(1) else null }.toList()
            }
        }

    companion object {
        const val STORED_FILM_ID = 1_000_000L

        @Container
        val mariaDb = KMariaDbContainer()
            .withUsername("root")
            .withPassword("")!!

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mariaDb.jdbcUrl }
        }
    }
}
