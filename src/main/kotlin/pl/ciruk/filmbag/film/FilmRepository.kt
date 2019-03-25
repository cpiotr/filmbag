package pl.ciruk.filmbag.film

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Repository
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.sql.DataSource

@Repository
class FilmRepository(@Autowired val entityManager: EntityManager, @Autowired val dataSource: DataSource)
    : SimpleJpaRepository<Film, Long>(Film::class.java, entityManager) {
    @PostConstruct
    fun load() {
        Database.connect(dataSource)
        transaction {
            for (film in Films.selectAll()) {
                println("${film[Films.title]}: ${film[Films.hash]}")
            }
        }
    }
}
