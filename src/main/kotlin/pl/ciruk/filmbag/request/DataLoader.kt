package pl.ciruk.filmbag.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.config.logConfiguration
import pl.ciruk.filmbag.function.runWithFallback
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.annotation.PostConstruct
import kotlin.math.max


@Service
class DataLoader(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal,
        private val httpClient: OkHttpClient,
        private val mapper: ObjectMapper,
        @Value("\${external.provider.filmrequest.url}") private val url: String,
        @Value("\${external.provider.filmrequest.limit:50}") private val limit: Int) {
    private val logger = KotlinLogging.logger {}
    private val threadPool = Executors.newSingleThreadExecutor()

    @PostConstruct
    fun logConfiguration() {
        logger.logConfiguration("Film provider URL", url)
        logger.logConfiguration("Film provider limit", limit)
    }

    fun importAsync(offset: Int = 0, cancelled: AtomicBoolean = AtomicBoolean(false)): CompletableFuture<Int> {
        logger.info("Import from offset: $offset")

        return CompletableFuture
                .supplyAsync(Supplier { loadDataOrThrow(offset, cancelled) }, threadPool)
                .exceptionally { runWithFallback(0) { logError(it) } }
    }

    private fun loadDataOrThrow(offset: Int, cancelled: AtomicBoolean): Int {
        logger.info("Loading data from: $url")
        val films = generateSequenceOfFilms(offset + 1, cancelled)
        val processedPagesCount = recordAndStoreUpToLimit(films)
        val lastPageIndex = max(offset + processedPagesCount - 1, 0)
        logger.info("Finished loading data. Last page was: $lastPageIndex")
        return lastPageIndex
    }

    fun recordAndStoreUpToLimit(sequenceOfFilmBatches: Sequence<List<FilmRequest>>): Int {
        var lastPage = 0
        var numberOfResults = 0
        for (filmBatch in sequenceOfFilmBatches) {
            if (filmBatch.isEmpty()) {
                logger.info("Got empty collection of films. Canceling. ")
                return lastPage
            }

            lastPage += 1
            numberOfResults += filmBatch.size
            logger.info { "Fetched ${filmBatch.size} films. Total number: $numberOfResults" }
            recordAndStore(filmBatch)
            if (numberOfResults >= limit) {
                return lastPage
            }
        }
        return lastPage
    }

    private fun recordAndStore(filmRequests: List<FilmRequest>) {
        journal.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }

    private fun generateSequenceOfFilms(offset: Int, cancelled: AtomicBoolean): Sequence<List<FilmRequest>> {
        return generateSequence(offset) { it + 1 }
                .takeWhile { !cancelled.get() }
                .map { fetchFilmsFromPage(it, cancelled) }
    }

    private fun fetchFilmsFromPage(index: Int, cancelled: AtomicBoolean): List<FilmRequest> {
        if (Thread.currentThread().isInterrupted) {
            return emptyList()
        }

        logger.info { "Fetch films from $index page" }

        val currentUrl = url.toHttpUrl()
                .newBuilder()
                .addPathSegment("$index")
                .build()
        val request: Request = Request.Builder()
                .url(currentUrl)
                .get()
                .build()
        return httpClient.newCall(request)
                .execute()
                .use { response -> response.parse() }
    }

    private fun logError(error: Throwable) {
        logger.error(error) { "Error while loading data" }
    }

    private fun Response.parse(): List<FilmRequest> {
        return if (this.isSuccessful) {
            this.body
                    ?.parse()
                    .orEmpty()
        } else {
            emptyList()
        }
    }

    private fun ResponseBody.parse(): List<FilmRequest> {
        return mapper.readValue(this.string())
    }
}
