package pl.ciruk.filmbag.film

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.springframework.stereotype.Component

/**
 * Fills the identity hash of films stored before V011 added the column. The hash is computed in code,
 * exactly as for new films, so that V016 can merge films sharing it.
 */
@Component
class V015__BackfillFilmIdentityHash : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val films = mutableMapOf<Long, FilmIdentity>()
        context.connection.createStatement().use { statement ->
            val rows = statement.executeQuery(
                """
                SELECT film.id, film.title, film.year, genre.name
                FROM film
                LEFT JOIN film_genres ON film_genres.film_id = film.id
                LEFT JOIN genre ON genre.id = film_genres.genres_id
                WHERE film.identity_hash IS NULL
                """
            )
            while (rows.next()) {
                val film = films.getOrPut(rows.getLong(1)) { FilmIdentity(rows.getString(2), rows.getInt(3)) }
                rows.getString(4)?.let { film.genreNames.add(it) }
            }
        }

        context.connection.prepareStatement("UPDATE film SET identity_hash = ? WHERE id = ?").use { update ->
            for ((id, film) in films) {
                update.setString(1, identityHashOf(film.title, film.year, film.genreNames))
                update.setLong(2, id)
                update.addBatch()
            }
            update.executeBatch()
        }
    }

    private class FilmIdentity(val title: String, val year: Int, val genreNames: MutableList<String> = mutableListOf())
}
