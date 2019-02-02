package pl.ciruk.filmbag.film

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct


@Repository
interface GenreRepository : CrudRepository<Genre, Long>

@Service
class GenreService(private val genreRepository: GenreRepository) {
    private val genreByName = HashMap<String, Genre>()

    @PostConstruct
    private fun loadExisting() {
        cacheAll(genreRepository.findAll())
    }

    fun findAll(): Iterable<Genre> {
        return genreRepository.findAll()
    }

    fun merge(genres: Iterable<String>): Set<Genre> {
        val newGenres = genres.filterNot { genreByName.containsKey(it) }.map { Genre(name = it) }
        val existingGenres = genres.mapNotNull { genreByName[it] }
        return (store(newGenres) + existingGenres).toSet()
    }

    private fun store(genres: Iterable<Genre>): Iterable<Genre> {
        val saved = genreRepository.saveAll(genres)
        cacheAll(saved)
        return saved.toSet()
    }

    private fun cacheAll(genres: Iterable<Genre>) {
        genres.forEach { genreByName[it.name] = it }
    }
}