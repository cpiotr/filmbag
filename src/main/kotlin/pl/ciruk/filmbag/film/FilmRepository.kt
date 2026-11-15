package pl.ciruk.filmbag.film

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface FilmRepository : JpaRepository<Film, Long>, JpaSpecificationExecutor<Film>
