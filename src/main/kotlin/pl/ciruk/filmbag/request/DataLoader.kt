package pl.ciruk.filmbag.request

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import java.lang.invoke.MethodHandles
import javax.annotation.PostConstruct

@Service
class DataLoader(
        private val requestProcessor: RequestProcessor,
        private val requestRecorder: RequestRecorder,
        @Value("\${external.provider.filmrequest.url}") private val url: String,
        @Value("\${external.provider.filmrequest.limit:50}") private val limit: Int) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    @PostConstruct
    fun load() {
        try {
            loadDataOrThrow()
        } catch (error: FuelError) {
            val response = error.response
            if (response.statusCode > -1) {
                log.error("Error while loading data: {}/{}", response.statusCode, response.responseMessage)
            } else {
                log.error("Error while loading data", error)
            }
        }
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
        requestRecorder.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }

    fun generateSequenceOfFilms(): Sequence<List<FilmRequest>> {
        return generateSequence(1) { it + 1 }
                .map { fetchFilmsFromPage(it) }
    }

    fun fetchFilmsFromPage(index: Int): List<FilmRequest> {
        log.debug("Fetch films from {} page", index)

        val (_, _, result) = "$url/$index".httpGet()
                .timeout(1_000)
                .timeoutRead(30_000)
                .responseObject<List<FilmRequest>>()
        return result.fold(
                { it },
                { error -> throw error }
        )
    }
}
