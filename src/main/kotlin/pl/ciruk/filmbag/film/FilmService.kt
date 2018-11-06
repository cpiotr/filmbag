package pl.ciruk.filmbag.film

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.annotation.PostConstruct

@Service
@Transactional
class FilmService(private val repository: FilmRepository) {
    private val recordedFilms = HashSet<Int>()

    fun store(film: Film) {
        if (recordedFilms.contains(film.hash)) {
            return
        }

        repository.save(film)
    }

    fun storeAll(films: List<Film>) {
        val notRecorded = films.filterNot { recordedFilms.contains(it.hash) }
        repository.saveAll(notRecorded)
    }

    fun findAll(): List<Film> {
        return repository.findAll().toList()
    }

    @PostConstruct
    fun load() {
        repository.findAll()
                .map { it.hash }
                .forEach { recordedFilms.add(it) }
    }
}
