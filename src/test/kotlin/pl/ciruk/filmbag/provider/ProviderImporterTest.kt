package pl.ciruk.filmbag.provider

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
import pl.ciruk.filmbag.film.FilmData
import pl.ciruk.filmbag.film.FilmStore
import pl.ciruk.filmbag.journal.Journal
import pl.ciruk.filmbag.testFilm
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class ProviderImporterTest {
    private lateinit var filmStore: FilmStore
    private lateinit var journal: Journal
    private val providers = mutableListOf<HttpServer>()

    @BeforeEach
    fun setUp() {
        filmStore = mock(FilmStore::class.java)
        journal = mock(Journal::class.java)
    }

    @AfterEach
    fun stopProviders() {
        providers.forEach { it.stop(0) }
    }

    @Test
    fun `should combine films from different batches up to given limit`() {
        val providerImporter = createDataLoader(3)

        val firstFilm = testFilm(year = 2010)
        val secondFilm = testFilm(year = 2011)
        val thirdFilm = testFilm(year = 2012)
        val fourthFilm = testFilm(year = 2013)
        val listOfResults = listOf(
                listOf(firstFilm, secondFilm),
                listOf(thirdFilm),
                listOf(fourthFilm)
        )
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = providerImporter.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(2)
        verify(filmStore).storeAll(listOf(firstFilm, secondFilm))
        verify(filmStore).storeAll(listOf(thirdFilm))
        verifyNoMoreInteractions(filmStore)
    }

    @Test
    fun `should combine films from different batches over a limit to avoid processing batch partially`() {
        val providerImporter = createDataLoader(3)

        val firstFilm = testFilm(year = 2010)
        val secondFilm = testFilm(year = 2011)
        val thirdFilm = testFilm(year = 2012)
        val fourthFilm = testFilm(year = 2013)
        val listOfResults = listOf(
                listOf(firstFilm, secondFilm),
                listOf(thirdFilm, fourthFilm)
        )
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = providerImporter.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(2)
        verify(filmStore).storeAll(listOf(firstFilm, secondFilm))
        verify(filmStore).storeAll(listOf(thirdFilm, fourthFilm))
        verifyNoMoreInteractions(filmStore)
    }

    @Test
    fun `should not fetch more batches if limit reached`() {
        val providerImporter = createDataLoader(2)

        val firstFilm = testFilm(year = 2010)
        val secondFilm = testFilm(year = 2011)
        val thirdFilm = testFilm(year = 2012)
        @Suppress("UNCHECKED_CAST")
        val supplier = mock(Supplier::class.java) as Supplier<List<FilmData>>
        `when`(supplier.get()).thenReturn(listOf(firstFilm, secondFilm)).thenReturn(listOf(thirdFilm))

        val sequenceOfResults = generateSequence { supplier.get() }


        val numberOfProcessedPages = providerImporter.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(1)
        verify(filmStore).storeAll(listOf(firstFilm, secondFilm))
        verify(supplier).get()
        verifyNoMoreInteractions(filmStore)
    }

    @Test
    fun `should store films until empty batch is received`() {
        val providerImporter = createDataLoader(10)

        val firstFilm = testFilm(year = 2010)
        val secondFilm = testFilm(year = 2011)
        val thirdFilm = testFilm(year = 2012)
        val fourthFilm = testFilm(year = 2013)
        val listOfResults = listOf(
                listOf(firstFilm, secondFilm),
                listOf(thirdFilm, fourthFilm),
                listOf()
        )
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = providerImporter.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(2)
        verify(filmStore).storeAll(listOf(firstFilm, secondFilm))
        verify(filmStore).storeAll(listOf(thirdFilm, fourthFilm))
        verifyNoMoreInteractions(filmStore)
    }

    @Test
    fun `should record only films that were stored`() {
        val providerImporter = createDataLoader(10)
        val storedFilms = listOf(testFilm(year = 2010))
        val failingFilms = listOf(testFilm(year = 2011))
        doThrow(IllegalStateException("Storing failed")).`when`(filmStore).storeAll(failingFilms)

        assertThatThrownBy { providerImporter.recordAndStoreUpToLimit(sequenceOf(storedFilms, failingFilms)) }
            .hasMessage("Storing failed")

        verify(journal).record(storedFilms)
        verifyNoMoreInteractions(journal)
    }

    @Test
    fun `should indicate no processed pages`() {
        val providerImporter = createDataLoader(10)

        val listOfResults = listOf(
                listOf<FilmData>()
        )
        val sequenceOfResults = listOfResults.asSequence()

        val numberOfProcessedPages = providerImporter.recordAndStoreUpToLimit(sequenceOfResults)

        assertThat(numberOfProcessedPages).isEqualTo(0)
        verifyNoMoreInteractions(filmStore)
    }

    @Test
    fun `should fail import when film provider returns an error`() {
        val provider = startProvider { it.respond(503, "") }
        val providerImporter = createDataLoader(10, provider.filmsUrl())

        val importResult = providerImporter.importAsync()

        assertThatThrownBy { importResult.join() }
            .hasCauseInstanceOf(FilmProviderException::class.java)
            .hasMessageContaining("HTTP 503")
        verifyNoInteractions(filmStore)
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
        val providerImporter = createDataLoader(10, provider.filmsUrl(), SimpleAsyncTaskExecutor())

        val firstImport = providerImporter.importAsync()
        pageRequested.await(5, TimeUnit.SECONDS)

        assertThatThrownBy { providerImporter.importAsync() }
            .isInstanceOf(ImportAlreadyRunningException::class.java)
        release.countDown()
        firstImport.get(5, TimeUnit.SECONDS)
        assertThat(providerImporter.importAsync().get(5, TimeUnit.SECONDS)).isEqualTo(0)
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
        val providerImporter = createDataLoader(10, provider.filmsUrl(), SimpleAsyncTaskExecutor())

        val importResult = providerImporter.importAsync()
        pageRequested.await(5, TimeUnit.SECONDS)
        providerImporter.cancelImport()
        release.countDown()

        assertThat(importResult.get(5, TimeUnit.SECONDS)).isEqualTo(0)
        verify(filmStore, times(1)).storeAll(anyList())
    }

    private fun createDataLoader(
        filmProviderLimit: Int,
        providerUrl: String = "http://localhost/films",
        taskExecutor: TaskExecutor = SyncTaskExecutor()
    ): ProviderImporter {
        return ProviderImporter(
            filmStore, journal, OkHttpClient(), JsonMapper.builder().addModule(kotlinModule()).build(), taskExecutor,
            FilmProviderProperties(URI(providerUrl), filmProviderLimit))
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
