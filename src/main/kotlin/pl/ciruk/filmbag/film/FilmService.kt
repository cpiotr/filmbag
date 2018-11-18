package pl.ciruk.filmbag.film

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    fun findAll(): List<Film> {
        return repository.findAll().toList()
    }

    @PostConstruct
    fun load() {
        repository.findAll()
                .map { it.hash }
                .forEach { existingFilmHashes.add(it) }
    }
}
