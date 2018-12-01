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
        @Value("\${external.provider.filmrequest.url}") private val url: String) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    @PostConstruct
    fun load() {
        log.info("Loading films from: {}", url)
        generateSequenceOfFilms()
                .take(5)
                .flatMap { it.asSequence() }
                .chunked(10)
                .forEach(::process)
    }

    fun process(filmRequests: List<FilmRequest>) {
        requestRecorder.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }

    fun generateSequenceOfFilms(): Sequence<List<FilmRequest>> {
        return generateSequence(1) { it + 1 }
                .map { fetchFilmsFromPage(it) }
    }

    fun fetchFilmsFromPage(index: Int): List<FilmRequest> {
        val (_, _, result) = "$url/$index"
                .httpGet()
                .timeout(1_000)
                .timeoutRead(2_000)
                .responseObject<List<FilmRequest>>()
        return result.fold(
                { it },
                { error -> error.logAndFallback(emptyList()) }
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        fun <T> FuelError.logAndFallback(list: List<T>): List<T> {
            log.info("Error while getting films {}/{}", this.response.statusCode, this.response.responseMessage)
            log.debug("Error details", this)
            return list
        }
    }
}
