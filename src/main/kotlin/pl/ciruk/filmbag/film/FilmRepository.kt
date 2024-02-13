package pl.ciruk.filmbag.film

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Repository
import jakarta.persistence.EntityManager

@Repository
class FilmRepository(@Autowired val entityManager: EntityManager)
    : SimpleJpaRepository<Film, Long>(Film::class.java, entityManager)
