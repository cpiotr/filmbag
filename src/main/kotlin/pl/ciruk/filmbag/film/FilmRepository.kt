package pl.ciruk.filmbag.film

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FilmRepository : CrudRepository<Film, Long>
