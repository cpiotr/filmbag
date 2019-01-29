package pl.ciruk.filmbag.film

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.boundary.*
import pl.ciruk.filmbag.boundary.ClosedRange
import java.math.BigDecimal
import javax.annotation.PostConstruct

@Service
@Transactional
class FilmService(private val repository: FilmRepository) {
    private val existingFilmHashes = HashSet<Int>()

    fun store(film: Film) {
        val added = existingFilmHashes.add(film.hash)
        if (!added) {
            return
        }

        repository.save(film)
    }

    fun storeAll(films: List<Film>) {
        val notRecorded = films.filterNot { existingFilmHashes.contains(it.hash) }
        repository.saveAll(notRecorded)
    }

    fun find(year: Range<Int> = EmptyRange(), score: Range<BigDecimal> = EmptyRange()): List<Film> {
        val allSpecifications = listOf(Pair(year, "year"), Pair(score, "score"))
                .mapNotNull { createSpecification(it.first, it.second) }

        return if (allSpecifications.isEmpty()) {
            repository.findAll()
        } else {
            repository.findAll(
                    allSpecifications.reduce { specification, other -> specification.and(other) }
            )
        }
    }

    private fun <T : Comparable<T>> createSpecification(range: Range<T>, propertyName: String): Specification<Film>? {
        return when (range) {
            is LeftClosedRange -> Specification { film, _, builder -> builder.greaterThanOrEqualTo(film[propertyName], range.from) }
            is RightClosedRange -> Specification { film, _, builder -> builder.lessThan(film[propertyName], range.to) }
            is ClosedRange -> Specification { film, _, builder -> builder.between(film[propertyName], range.from, range.to) }
            is EmptyRange -> null
        }
    }

    @PostConstruct
    private fun load() {
        repository.findAll()
                .map { it.hash }
                .forEach { existingFilmHashes.add(it) }
    }
}
