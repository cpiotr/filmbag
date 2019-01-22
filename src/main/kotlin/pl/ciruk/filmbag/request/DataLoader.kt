package pl.ciruk.filmbag.request

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.jackson.responseObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.config.asHttpGet
import pl.ciruk.filmbag.function.logWithoutFallback
import java.lang.invoke.MethodHandles
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class DataLoader(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal,
        @Value("\${external.provider.filmrequest.url}") private val url: String,
        @Value("\${external.provider.filmrequest.limit:50}") private val limit: Int) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val threadPool = Executors.newSingleThreadExecutor()

    fun importAsync() {
        CompletableFuture
                .runAsync(Runnable { loadDataOrThrow() }, threadPool)
                .exceptionally { logWithoutFallback { logError(it) } }
    }

    private fun loadDataOrThrow() {
        log.info("Loading data from: {}", url)
        generateSequenceOfFilms()
                .flatMap { it.asSequence() }
                .take(limit)
                .chunked(10)
                .forEach(::recordAndStore)
    }

    fun recordAndStore(filmRequests: List<FilmRequest>) {
        journal.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }

    fun generateSequenceOfFilms(): Sequence<List<FilmRequest>> {
        return generateSequence(1) { it + 1 }
                .map { fetchFilmsFromPage(it) }
    }

    fun fetchFilmsFromPage(index: Int): List<FilmRequest> {
        log.debug("Fetch films from {} page", index)

        val (_, _, result) = "$url/$index".asHttpGet()
                .responseObject<List<FilmRequest>>()
        return result.fold(
                { it },
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
