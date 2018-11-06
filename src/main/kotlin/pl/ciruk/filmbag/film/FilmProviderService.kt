package pl.ciruk.filmbag.film

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.RequestAdapter
import javax.annotation.PostConstruct

@Service
class FilmProviderService(private val requestAdapter: RequestAdapter, private val filmService: FilmService) {
    @PostConstruct
    fun load() {
        generateSequenceOfFilms()
                .take(30)
                .chunked(10)
                .forEach { filmService.storeAll(it) }
    }

    fun generateSequenceOfFilms(): Sequence<Film> {
        return generateSequence(1) { it + 1 }
                .flatMap { fetchFilmsFromPage(it).asSequence() }
    }

    fun fetchFilmsFromPage(index: Int): List<Film> {
        val (_, _, result) = "http://localhost:8080/resources/suggestions/$index"
                .httpGet()
                .responseObject<List<FilmRequest>>()
        return result.fold(
                { it.map(requestAdapter::convertToFilm) },
                { error -> emptyList() }
        )
    }
}