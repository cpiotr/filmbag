package pl.ciruk.filmbag.film

import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.jackson.responseObject
import pl.ciruk.filmbag.boundary.FilmRequest

@Service
class FilmService(private val repository: FilmRepository) {
    fun store(film: Film) {
        repository.save(film)
    }

    fun findAll(): List<Film> {
        return repository.findAll().toList()
    }

    @PostConstruct
    fun load() {
        generateSequenceOfFilms()
                .take(15)
                .forEach { println(it) }
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
                { it.map(FilmRequest::toFilm) },
                { error -> emptyList() }
        )
    }
}
