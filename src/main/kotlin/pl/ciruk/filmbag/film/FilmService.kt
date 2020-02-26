package pl.ciruk.filmbag.film

import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.boundary.*
import pl.ciruk.filmbag.boundary.ClosedRange
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Service
class FilmService(private val repository: FilmRepository) {
    private val logger = KotlinLogging.logger {}

    private val existingFilmHashes = ConcurrentHashMap.newKeySet<Int>()

    @Transactional
    fun storeAll(films: List<Film>) {
        val filmsToBeStored = films.filter { existingFilmHashes.add(it.hash) }
        repository.saveAll(filmsToBeStored)
        logger.info { "Stored ${filmsToBeStored.size} films" }
    }

    @Transactional
    fun find(
            year: Range<Int> = EmptyRange(),
            score: Range<BigDecimal> = EmptyRange(),
            page: Int = 0,
            pageSize: Int = 10): List<Film> {
        logger.info { "Find by year=$year; score=$score" }

        val allSpecifications = listOf(Pair(year, "year"), Pair(score, "score"))
                .mapNotNull { createSpecification(it.first, it.second) }

        val pageRequest = PageRequest.of(page, pageSize)

        return if (allSpecifications.isEmpty()) {
            repository
                    .findAll(pageRequest)
                    .content
        } else {
            val specification = allSpecifications.reduce { first, second -> first.and(second)!! }
            repository
                    .findAll(specification, pageRequest)
                    .content
        }
    }

    private fun <T : Comparable<T>> createSpecification(range: Range<T>, propertyName: String): Specification<Film>? {
        return when (range) {
            is LeftClosedRange -> Specification { film, _, builder -> builder.greaterThanOrEqualTo(film[propertyName], range.from) }
            is RightClosedRange -> Specification { film, _, builder -> builder.lessThanOrEqualTo(film[propertyName], range.to) }
            is ClosedRange -> Specification { film, _, builder -> builder.between(film[propertyName], range.from, range.to) }
            is EmptyRange -> null
        }
    }

    @PostConstruct
    @Transactional
    private fun load() {
        val films = repository.findAll()
        val updatedFilms = films.map { film -> copyUpdatingHash(film) }
                .sortedByDescending { it.created }
                .partition { existingFilmHashes.add(it.hash) }
        repository.deleteAll(updatedFilms.second)
        repository.saveAll(updatedFilms.first)
        repository.flush()
    }

    private fun copyUpdatingHash(film: Film): Film {
        return Film(
                film.id,
                film.created,
                film.title, film.year,
                film.plot,
                film.link,
                film.poster,
                film.score,
                film.scores,
                film.genres,
                Objects.hash(film.title, film.year, film.link))
    }
}
