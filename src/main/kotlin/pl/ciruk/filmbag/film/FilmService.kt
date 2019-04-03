package pl.ciruk.filmbag.film

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.boundary.*
import pl.ciruk.filmbag.boundary.ClosedRange
import java.lang.invoke.MethodHandles
import java.math.BigDecimal
import javax.annotation.PostConstruct
import javax.sql.DataSource

@Service
@Transactional
class FilmService(private val repository: FilmRepository, private val dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

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

    fun find(
            year: Range<Int> = EmptyRange(),
            score: Range<BigDecimal> = EmptyRange(),
            page: Int = 0,
            pageSize: Int = 10): List<Film> {
        logger.info("Find by year=$year; score=$score")

        val allClauses = listOfNotNull(createSpecification2(score, Films.score), createSpecification2(year, Films.year))

        Database.connect(dataSource)
        return if (allClauses.isEmpty()) {
            transaction {
                Films.selectAll()
                        .limit(pageSize, pageSize * page)
                        .map { convertToFilm(it) }
            }
        } else {
            transaction {
                Films.innerJoin(Scores, onColumn = { Films.id }, otherColumn = { Scores.filmId })
                        .select(allClauses.reduceRight(Op<Boolean>::and))
                        .limit(pageSize, pageSize * page)
                        .map { convertToFilm(it) }
            }
        }
    }

    private fun <T : Comparable<T>> createSpecification2(range: Range<T>, column: Column<out T>): Op<Boolean>? {
        return when (range) {
            is LeftClosedRange -> Op.build { column greaterEq range.from }
            is RightClosedRange -> Op.build { column lessEq range.to }
            is ClosedRange -> Op.build { column.between(range.from, range.to) }
            is EmptyRange -> null
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
    private fun load() {
        repository.findAll()
                .map { it.hash }
                .forEach { existingFilmHashes.add(it) }
    }
}
