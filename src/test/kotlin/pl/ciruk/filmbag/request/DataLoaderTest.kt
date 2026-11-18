package pl.ciruk.filmbag.request

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.testFilmRequest
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class DataLoaderTest {
    private lateinit var requestProcessor: RequestProcessor
    private lateinit var journal: Journal
    private val providers = mutableListOf<HttpServer>()

    @BeforeEach
    fun setUp() {
        requestProcessor = mock(RequestProcessor::class.java)
        journal = mock(Journal::class.java)
    }

    @AfterEach
    fun stopProviders() {
        providers.forEach { it.stop(0) }
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
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(2)
        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm))
        verify(requestProcessor).storeAll(listOf(thirdFilm))
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
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(2)
        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm))
        verify(requestProcessor).storeAll(listOf(thirdFilm, fourthFilm))
        verifyNoMoreInteractions(requestProcessor)
    }

    @Test
    fun `should not fetch more batches if limit reached`() {
        val dataLoader = createDataLoader(2)

        val firstFilm = testFilmRequest(year = 2010)
        val secondFilm = testFilmRequest(year = 2011)
        val thirdFilm = testFilmRequest(year = 2012)
        @Suppress("UNCHECKED_CAST")
        val supplier = mock(Supplier::class.java) as Supplier<List<FilmRequest>>
        `when`(supplier.get()).thenReturn(listOf(firstFilm, secondFilm)).thenReturn(listOf(thirdFilm))

        val sequenceOfResults = generateSequence { supplier.get() }


        val numberOfProcessedPages = dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(1)
        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm))
        verify(supplier).get()
        verifyNoMoreInteractions(requestProcessor)
    }

    @Test
    fun `should store films until empty batch is received`() {
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
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(2)
        verify(requestProcessor).storeAll(listOf(firstFilm, secondFilm))
        verify(requestProcessor).storeAll(listOf(thirdFilm, fourthFilm))
        verifyNoMoreInteractions(requestProcessor)
    }

    @Test
    fun `should record only films that were stored`() {
        val dataLoader = createDataLoader(10)
        val storedFilms = listOf(testFilmRequest(year = 2010))
        val failingFilms = listOf(testFilmRequest(year = 2011))
        doThrow(IllegalStateException("Storing failed")).`when`(requestProcessor).storeAll(failingFilms)

        assertThatThrownBy { dataLoader.recordAndStoreUpToLimit(sequenceOf(storedFilms, failingFilms)) }
            .hasMessage("Storing failed")

        verify(journal).record(storedFilms)
        verifyNoMoreInteractions(journal)
    }

    @Test
    fun `should indicate no processed pages`() {
        val dataLoader = createDataLoader(10)

        val listOfResults = listOf(
                listOf<FilmRequest>()
        )
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = dataLoader.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(0)
        verifyNoMoreInteractions(requestProcessor)
    }

    @Test
    fun `should fail import when film provider returns an error`() {
        val provider = startProvider { it.respond(503, "") }
        val dataLoader = createDataLoader(10, provider.filmsUrl())

        val importResult = dataLoader.importAsync()

        assertThatThrownBy { importResult.join() }
            .hasCauseInstanceOf(IOException::class.java)
            .hasMessageContaining("HTTP 503")
        verifyNoInteractions(requestProcessor)
    }

    @Test
    fun `should reject a second import while one is running`() {
        val pageRequested = CountDownLatch(1)
        val release = CountDownLatch(1)
        val provider = startProvider {
            pageRequested.countDown()
            release.await()
            it.respond(200, "[]")
        }
        val dataLoader = createDataLoader(10, provider.filmsUrl(), SimpleAsyncTaskExecutor())

        val firstImport = dataLoader.importAsync()
        pageRequested.await(5, TimeUnit.SECONDS)

        assertThatThrownBy { dataLoader.importAsync() }
            .isInstanceOf(ImportAlreadyRunningException::class.java)
        release.countDown()
        firstImport.get(5, TimeUnit.SECONDS)
        assertThat(dataLoader.importAsync().get(5, TimeUnit.SECONDS)).isEqualTo(0)
    }

    @Test
    fun `should stop import after the current page when cancelled`() {
        val pageRequested = CountDownLatch(1)
        val release = CountDownLatch(1)
        val provider = startProvider {
            pageRequested.countDown()
            release.await()
            it.respond(200, """[{"title": "Film", "year": 2000, "link": "http://film", "score": 0.5}]""")
        }
        val dataLoader = createDataLoader(10, provider.filmsUrl(), SimpleAsyncTaskExecutor())

        val importResult = dataLoader.importAsync()
        pageRequested.await(5, TimeUnit.SECONDS)
        dataLoader.cancelImport()
        release.countDown()

        assertThat(importResult.get(5, TimeUnit.SECONDS)).isEqualTo(0)
        verify(requestProcessor, times(1)).storeAll(anyList())
    }

    private fun createDataLoader(
        filmProviderLimit: Int,
        providerUrl: String = "http://localhost/films",
        taskExecutor: TaskExecutor = SyncTaskExecutor()
    ): DataLoader {
        return DataLoader(
            requestProcessor, journal, OkHttpClient(), jacksonObjectMapper(), taskExecutor, providerUrl, filmProviderLimit)
    }

    private fun startProvider(handler: (HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { handler(it) }
        server.start()
        providers.add(server)
        return server
    }

    private fun HttpServer.filmsUrl() = "http://127.0.0.1:${address.port}/films"

    private fun HttpExchange.respond(status: Int, body: String) {
        val bytes = body.toByteArray()
        sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
