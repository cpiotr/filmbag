package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mariadb.MariaDBContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@Testcontainers
class FilmIdentityMigrationTest {
    private val heatHash = identityHashOf("Heat", 1995, listOf("Crime", "Drama"))

    @Test
    fun `should fill missing identities and merge films sharing one into the newest`() {
        flyway().target("14").load().migrate()
        connect().use { db ->
            db.execute("INSERT INTO genre (id, name) VALUES (1, 'Crime'), (2, 'Drama')")
            // Films 1, 2 and 4 are one film; film 4 was stored by a version that already set the hash
            db.insertFilm(id = 1, title = "Heat", created = "2020-01-01", identityHash = null, genreIds = listOf(1, 2))
            db.insertFilm(id = 2, title = "Heat", created = "2021-01-01", identityHash = null, genreIds = listOf(2, 1))
            db.insertFilm(id = 3, title = "Heat", created = "2019-01-01", identityHash = null, genreIds = listOf(1))
            db.insertFilm(id = 4, title = "Heat", created = "2018-01-01", identityHash = heatHash, genreIds = listOf(1, 2))
            db.insertScore(id = 1, filmId = 1, type = "CRITIC", url = "http://critics")
            db.insertScore(id = 2, filmId = 2, type = "CRITIC", url = "http://critics")
            db.insertScore(id = 3, filmId = 4, type = "AMATEUR", url = null)
        }

        flyway().javaMigrations(V015__BackfillFilmIdentityHash()).load().migrate()

        connect().use { db ->
            assertThat(db.query("SELECT id, identity_hash FROM film ORDER BY id")).containsExactly(
                listOf("2", heatHash),
                listOf("3", identityHashOf("Heat", 1995, listOf("Crime"))))
            assertThat(db.query("SELECT film_id, type FROM score ORDER BY type")).containsExactly(
                listOf("2", "AMATEUR"),
                listOf("2", "CRITIC"))
            assertThat(db.query("SELECT film_id, genres_id FROM film_genres ORDER BY film_id, genres_id")).containsExactly(
                listOf("2", "1"),
                listOf("2", "2"),
                listOf("3", "1"))
            assertThatThrownBy { db.insertFilm(id = 5, title = "Heat", created = "2022-01-01", identityHash = heatHash, genreIds = emptyList()) }
                .isInstanceOf(SQLException::class.java)
                .hasMessageContaining("u_film_identity_hash")
        }
    }

    private fun flyway() = Flyway.configure()
        .dataSource(mariaDb.jdbcUrl, mariaDb.username, mariaDb.password)

    private fun connect() = DriverManager.getConnection(mariaDb.jdbcUrl, mariaDb.username, mariaDb.password)

    private fun Connection.execute(sql: String) = createStatement().use { it.execute(sql) }

    private fun Connection.insertFilm(id: Int, title: String, created: String, identityHash: String?, genreIds: List<Int>) {
        prepareStatement("INSERT INTO film (id, title, year, link, score, created, identity_hash) VALUES (?, ?, 1995, 'link', 0.8, ?, ?)").use {
            it.setInt(1, id)
            it.setString(2, title)
            it.setString(3, created)
            it.setString(4, identityHash)
            it.executeUpdate()
        }
        genreIds.forEach { execute("INSERT INTO film_genres (film_id, genres_id) VALUES ($id, $it)") }
    }

    private fun Connection.insertScore(id: Int, filmId: Int, type: String, url: String?) {
        prepareStatement("INSERT INTO score (id, film_id, grade, quantity, type, url) VALUES (?, ?, 0.8, 10, ?, ?)").use {
            it.setInt(1, id)
            it.setInt(2, filmId)
            it.setString(3, type)
            it.setString(4, url)
            it.executeUpdate()
        }
    }

    private fun Connection.query(sql: String): List<List<String>> = createStatement().use { statement ->
        val rows = statement.executeQuery(sql)
        val columns = rows.metaData.columnCount
        generateSequence { if (rows.next()) (1..columns).map { rows.getString(it) } else null }.toList()
    }

    companion object {
        @Container
        val mariaDb = MariaDBContainer("mariadb:11.4")
    }
}
