package pl.ciruk.filmbag.film

import org.springframework.stereotype.Service

@Service
class FilmService(private val repository: FilmRepository) {
    fun store(film: Film) {
        repository.save(film)
    }

    fun findAll(): List<Film> {
        return repository.findAll().toList()
    }
}