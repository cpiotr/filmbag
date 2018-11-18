package pl.ciruk.filmbag.request

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import javax.annotation.PostConstruct

@Service
class DataLoader(
        private val requestProcessor: RequestProcessor,
        private val requestRecorder: RequestRecorder) {
    @PostConstruct
    fun load() {
        generateSequenceOfFilms()
                .take(30)
                .chunked(10)
                .forEach(::process)
    }

    fun process(filmRequests: List<FilmRequest>) {
        requestRecorder.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }

    fun generateSequenceOfFilms(): Sequence<FilmRequest> {
        return generateSequence(1) { it + 1 }
                .flatMap { fetchFilmsFromPage(it).asSequence() }
    }

    fun fetchFilmsFromPage(index: Int): List<FilmRequest> {
        val (_, _, result) = "http://localhost:8080/resources/suggestions/$index"
                .httpGet()
                .responseObject<List<FilmRequest>>()
        return result.fold(
                { it },
                { error -> emptyList() }
        )
    }
}
