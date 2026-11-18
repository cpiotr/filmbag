package pl.ciruk.filmbag.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.film.FilmData
import pl.ciruk.filmbag.film.FilmStore
import pl.ciruk.filmbag.journal.Journal
import kotlin.math.max
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

@Service
class ProviderImporter(
        private val filmStore: FilmStore,
        private val journal: Journal,
        private val httpClient: OkHttpClient,
        private val mapper: ObjectMapper,
        private val taskExecutor: TaskExecutor,
        private val provider: FilmProviderProperties) {
    private val logger = KotlinLogging.logger {}

    /** Cancellation flag of the running import, or null when no import runs. */
    private val runningImportCancelled = AtomicReference<AtomicBoolean?>(null)

    /** @throws ImportAlreadyRunningException when another import has not finished yet */
    fun importAsync(offset: Int = 0): CompletableFuture<Int> {
        val cancelled = AtomicBoolean(false)
        if (!runningImportCancelled.compareAndSet(null, cancelled)) {
            throw ImportAlreadyRunningException()
        }
        logger.info { "Import from offset: $offset" }

        return CompletableFuture
                .supplyAsync(Supplier { importPages(offset, cancelled) }, taskExecutor)
                .whenComplete { _, error ->
                    runningImportCancelled.set(null)
                    error?.let { logger.error(it) { "Error while loading data" } }
                }
    }

    fun cancelImport() {
        logger.info { "Cancelling import" }

        runningImportCancelled.get()?.set(true)
    }

    private fun importPages(offset: Int, cancelled: AtomicBoolean): Int {
        logger.info { "Loading data from: ${provider.url}" }

        val films = generateSequenceOfFilms(offset + 1, cancelled)
        val processedPagesCount = recordAndStoreUpToLimit(films)
        val lastPageIndex = max(offset + processedPagesCount - 1, 0)
        logger.info { "Finished loading data. Last offset was: $lastPageIndex" }
        return lastPageIndex
    }

    fun recordAndStoreUpToLimit(sequenceOfFilmBatches: Sequence<List<FilmData>>): Int {
        var lastPage = 0
        var numberOfResults = 0
        for (filmBatch in sequenceOfFilmBatches) {
            if (filmBatch.isEmpty()) {
                logger.info { "Got empty collection of films. Canceling. " }
                return lastPage
            }

            lastPage += 1
            numberOfResults += filmBatch.size
            logger.info { "Fetched ${filmBatch.size} films. Total number: $numberOfResults" }
            recordAndStore(filmBatch)
            if (numberOfResults >= provider.filmsPerImport) {
                return lastPage
            }
        }
        return lastPage
    }

    private fun recordAndStore(films: List<FilmData>) {
        filmStore.storeAll(films)
        journal.record(films)
    }

    private fun generateSequenceOfFilms(offset: Int, cancelled: AtomicBoolean): Sequence<List<FilmData>> {
        return generateSequence(offset) { it + 1 }
                .takeWhile { !cancelled.get() }
                .map { fetchFilmsFromPage(it) }
    }

    private fun fetchFilmsFromPage(index: Int): List<FilmData> {
        logger.info { "Fetch films from $index page" }

        val currentUrl = provider.url.toString().toHttpUrl()
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

    private fun Response.parse(): List<FilmData> {
        if (!this.isSuccessful) {
            throw IOException("Film provider returned HTTP ${this.code} for ${this.request.url}")
        }
        return this.body
                ?.parse()
                .orEmpty()
    }

    private fun ResponseBody.parse(): List<FilmData> {
        return mapper.readValue(this.string())
    }
}

class ImportAlreadyRunningException : IllegalStateException("An import is already running")
