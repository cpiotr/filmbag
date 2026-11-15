package pl.ciruk.filmbag.film

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


interface GenreRepository : CrudRepository<Genre, Long> {
    fun findAllByNameIn(names: Collection<String>): List<Genre>
}

@Service
class GenreService(private val genreRepository: GenreRepository) {
    fun findAll(): Iterable<Genre> {
        return genreRepository.findAll()
    }

    @Transactional
    fun merge(genreNames: Iterable<String>): Set<Genre> {
        val names = genreNames.toSet()
        val existingGenres = genreRepository.findAllByNameIn(names)
        val existingNames = existingGenres.map { it.name }.toSet()
        val newGenres = names.filterNot { it in existingNames }.map { Genre(name = it) }
        return (existingGenres + genreRepository.saveAll(newGenres)).toSet()
    }
}
