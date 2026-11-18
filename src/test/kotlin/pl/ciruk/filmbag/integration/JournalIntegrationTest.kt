package pl.ciruk.filmbag.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.JournalResource
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.ReplayResult
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.XAddParams

@IntegrationTest
class JournalIntegrationTest(
    @Autowired val journal: Journal,
    @Autowired val journalResource: JournalResource,
    @Autowired val requestProcessor: RequestProcessor,
    @Autowired val redisClient: RedisClient,
    @Autowired val testData: TestData
) {
    private val replayedFilms = mutableListOf<List<FilmRequest>>()

    @BeforeEach
    fun deleteAllData() {
        testData.deleteAll()
    }

    @Test
    fun `should replay recorded films in the order they were recorded`() {
        val filmRequest = testFilmRequest()
        val otherFilmRequest = testOtherFilmRequest()
        journal.record(listOf(filmRequest))
        journal.record(listOf(filmRequest, otherFilmRequest))
        journal.record(listOf(otherFilmRequest))

        val result = journal.replay { replayedFilms += it }

        assertThat(replayedFilms)
            .containsExactly(listOf(filmRequest), listOf(filmRequest, otherFilmRequest), listOf(otherFilmRequest))
        assertThat(result).isEqualTo(ReplayResult(replayed = 3, failed = emptyList()))
    }

    @Test
    fun `should replay more entries than fit on one page`() {
        val filmRequests = List(250) { testOtherFilmRequest(year = 1900 + it) }
        filmRequests.forEach { journal.record(listOf(it)) }

        val result = journal.replay { replayedFilms += it }

        assertThat(replayedFilms.flatten()).isEqualTo(filmRequests)
        assertThat(result.replayed).isEqualTo(250)
    }

    @Test
    fun `should replay empty journal`() {
        val result = journal.replay { replayedFilms += it }

        assertThat(replayedFilms).isEmpty()
        assertThat(result).isEqualTo(ReplayResult(replayed = 0, failed = emptyList()))
    }

    @Test
    fun `should report an unreadable entry and replay the others`() {
        val filmRequest = testFilmRequest()
        val unreadableEntry = redisClient.xadd(Journal.STREAM_KEY, XAddParams.xAddParams(), mapOf("entry" to "not JSON"))
        journal.record(listOf(filmRequest))

        val result = journal.replay { replayedFilms += it }

        assertThat(replayedFilms).containsExactly(listOf(filmRequest))
        assertThat(result).isEqualTo(ReplayResult(replayed = 1, failed = listOf(unreadableEntry.toString())))
    }

    @Test
    fun `should report an entry that could not be stored and replay the others`() {
        val filmRequest = testFilmRequest()
        journal.record(listOf(testOtherFilmRequest()))
        journal.record(listOf(filmRequest))

        val result = journal.replay { films ->
            check(films.single() == filmRequest) { "Storing failed" }
            replayedFilms += films
        }

        assertThat(replayedFilms).containsExactly(listOf(filmRequest))
        assertThat(result.replayed).isEqualTo(1)
        assertThat(result.failed).hasSize(1)
    }

    @Test
    fun `should record every stored film in a snapshot`() {
        val filmRequests = List(3) { testOtherFilmRequest(year = 2000 + it) }
        requestProcessor.storeAll(filmRequests)

        journalResource.snapshot()
        journal.replay { replayedFilms += it }

        assertThat(replayedFilms.flatten()).containsExactlyInAnyOrderElementsOf(filmRequests)
    }

    @Test
    fun `should record nothing in a snapshot of an empty database`() {
        journalResource.snapshot()

        assertThat(journal.replay { replayedFilms += it }.replayed).isZero()
    }
}
