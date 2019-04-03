package pl.ciruk.filmbag.film

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.sql.DataSource

@Repository
class FilmRepository(@Autowired val entityManager: EntityManager, @Autowired val dataSource: DataSource)
    : SimpleJpaRepository<Film, Long>(Film::class.java, entityManager) {
    @PostConstruct
    fun load() {
        val connect = Database.connect(dataSource)
        transaction {
            Films.select { Films.score greaterEq BigDecimal.valueOf(0.6) }
                    .limit(1, 0)
                    .forEach { println("pc: $it[Films.title]") }

            for (film in Films.selectAll()) {
                println("${film[Films.title]}: ${film[Films.hash]}")
            }
        }
    }
}
