package pl.ciruk.filmbag.request

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.jackson.responseObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.config.asHttpGet
import pl.ciruk.filmbag.function.logWithFallback
import java.lang.invoke.MethodHandles
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier

@Service
class DataLoader(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal,
        @Value("\${external.provider.filmrequest.url}") private val url: String,
        @Value("\${external.provider.filmrequest.limit:50}") private val limit: Int) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val threadPool = Executors.newSingleThreadExecutor()

    fun importAsync(offset: Int = 0): CompletableFuture<Int> {
        log.info("Import from offset: $offset")

        return CompletableFuture
                .supplyAsync(Supplier { loadDataOrThrow(offset) }, threadPool)
                .exceptionally { logWithFallback(0) { logError(it) } }
    }

    private fun loadDataOrThrow(offset: Int): Int {
        log.info("Loading data from: {}", url)
        var lastPage = offset + 1
        generateSequenceOfFilms(lastPage)
                .onEach { it.result.ifEmpty { log.info("Got empty collection of films. Canceling. ") } }
                .takeWhile { it.result.isNotEmpty() }
                .onEach { lastPage = it.index }
                .flatMap { it.result.asSequence() }
                .take(limit)
                .chunked(10)
                .forEach(this::recordAndStore)
        log.info("Finished loading data. Last page was: $lastPage")
        return lastPage
    }

    private fun recordAndStore(filmRequests: List<FilmRequest>) {
        journal.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }

    private fun generateSequenceOfFilms(offset: Int): Sequence<IndexedResult<List<FilmRequest>>> {
        return generateSequence(offset) { it + 1 }
                .map { fetchFilmsFromPage(it) }
    }

    private fun fetchFilmsFromPage(index: Int): IndexedResult<List<FilmRequest>> {
        log.debug("Fetch films from {} page", index)

        val (_, _, result) = "$url/$index".asHttpGet()
                .responseObject<List<FilmRequest>>()
        return result.fold(
                { IndexedResult(index, it) },
                { error -> throw error }
        )
    }

    private fun logError(error: Throwable) {
        if (error is FuelError && error.response.statusCode > -1) {
            val response = error.response

            log.error("Error while loading data: {}/{}", response.statusCode, response.responseMessage)
        } else {
            log.error("Error while loading data", error)
        }
    }
}

class IndexedResult<T>(val index: Int, val result: T)
