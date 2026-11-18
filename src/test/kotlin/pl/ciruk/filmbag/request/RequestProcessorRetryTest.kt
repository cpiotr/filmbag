package pl.ciruk.filmbag.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.identityHashOf
import pl.ciruk.filmbag.testOtherFilmRequest
import javax.sql.DataSource

@IntegrationTest
class RequestProcessorRetryTest(
    @Autowired val requestProcessor: RequestProcessor,
    @Autowired val dataSource: DataSource,
    @Autowired val testData: TestData
) {
    @MockitoSpyBean
    lateinit var filmService: FilmService

    @BeforeEach
    fun deleteAllData() {
        testData.deleteAll()
    }

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
        // A connection of its own, outside the transaction of the request under test
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.executeUpdate(
                    "INSERT INTO film (id, title, year, link, score, identity_hash) " +
                        "VALUES ($STORED_FILM_ID, 'Stored by another request', 1999, 'link', 0.5, '$identityHash')"
                )
            }
        }
    }

    private fun query(sql: String): List<Long> =
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                val rows = statement.executeQuery(sql)
                generateSequence { if (rows.next()) rows.getLong(1) else null }.toList()
            }
        }

    companion object {
        const val STORED_FILM_ID = 1_000_000L
    }
}
