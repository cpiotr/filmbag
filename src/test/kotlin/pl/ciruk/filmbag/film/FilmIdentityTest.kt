package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FilmIdentityTest {
    @Test
    fun `should identify film by title, year and genre names regardless of genre order`() {
        val hash = identityHashOf("Title", 1999, listOf("Drama", "Noir"))
        val sameFilm = identityHashOf("Title", 1999, listOf("Noir", "Drama"))

        assertThat(sameFilm).isEqualTo(hash)
        assertThat(hash).hasSize(64).matches("[0-9a-f]+")
    }

    @Test
    fun `should tell films apart by title, year and genres`() {
        val hash = identityHashOf("Title", 1999, listOf("Drama"))

        assertThat(identityHashOf("Other title", 1999, listOf("Drama"))).isNotEqualTo(hash)
        assertThat(identityHashOf("Title", 2000, listOf("Drama"))).isNotEqualTo(hash)
        assertThat(identityHashOf("Title", 1999, listOf("Drama", "Noir"))).isNotEqualTo(hash)
        assertThat(identityHashOf("Title", 1999, emptyList())).isNotEqualTo(hash)
    }

    @Test
    fun `should identify film by its genre names`() {
        val film = Film(title = "Title", year = 1999, link = "link", score = 0.5, genres = setOf(Genre(id = 7, name = "Noir")))

        assertThat(film.identityHash).isEqualTo(identityHashOf("Title", 1999, listOf("Noir")))
    }
}
