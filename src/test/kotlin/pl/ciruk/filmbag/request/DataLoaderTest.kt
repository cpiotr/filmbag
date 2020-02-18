package pl.ciruk.filmbag.request

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import pl.ciruk.filmbag.testFilmRequest

class DataLoaderTest() {
    private lateinit var requestProcessor: RequestProcessor
    private lateinit var journal: Journal
    @BeforeEach
    fun setUp() {
        requestProcessor = mock(pl.ciruk.filmbag.request.RequestProcessor::class.java)
        journal = mock(Journal::class.java)
    }

    @Test
    fun `should combine films from different batches up to given limit`() {
        val dataLoader = createDataLoader(3)

        val firstFilm = testFilmRequest(year = 2010)
        val secondFilm = testFilmRequest(year = 2011)
        val thirdFilm = testFilmRequest(year = 2012)
        val fourthFilm = testFilmRequest(year = 2013)
        val listOfResults = listOf(
                listOf(firstFilm, secondFilm),
                listOf(thirdFilm),
                listOf(fourthFilm)
        )
        val sequenceOfResults = listOfResults.mapIndexed { index, list -> IndexedResult(index, list) }
                .asSequence()

        dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm, thirdFilm))
        verifyNoMoreInteractions(requestProcessor)
    }

    @Test
    fun `should combine films from different batches over a limit to avoid processing batch partially`() {
        val dataLoader = createDataLoader(3)

        val firstFilm = testFilmRequest(year = 2010)
        val secondFilm = testFilmRequest(year = 2011)
        val thirdFilm = testFilmRequest(year = 2012)
        val fourthFilm = testFilmRequest(year = 2013)
        val listOfResults = listOf(
                listOf(firstFilm, secondFilm),
                listOf(thirdFilm, fourthFilm)
        )
        val sequenceOfResults = listOfResults.mapIndexed { index, list -> IndexedResult(index, list) }
                .asSequence()

        dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm, thirdFilm, fourthFilm))
        verifyNoMoreInteractions(requestProcessor)
    }

    @Test
    fun `should combine films until empty batch is received`() {
        val dataLoader = createDataLoader(10)

        val firstFilm = testFilmRequest(year = 2010)
        val secondFilm = testFilmRequest(year = 2011)
        val thirdFilm = testFilmRequest(year = 2012)
        val fourthFilm = testFilmRequest(year = 2013)
        val listOfResults = listOf(
                listOf(firstFilm, secondFilm),
                listOf(thirdFilm, fourthFilm),
                listOf()
        )
        val sequenceOfResults = listOfResults.mapIndexed { index, list -> IndexedResult(index, list) }
                .asSequence()

        dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm, thirdFilm, fourthFilm))
        verifyNoMoreInteractions(requestProcessor)
    }

    private fun createDataLoader(filmProviderLimit: Int): DataLoader {
        return DataLoader(requestProcessor, journal, "TestUrl", filmProviderLimit)
    }
}