package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FilmIdentityTest {
    private val drama = Genre(id = 1, name = "Drama")
    private val noir = Genre(id = 2, name = "Noir")

    @Test
    fun `should identify film by title, year and genre names regardless of genre order and ids`() {
        val hash = identityHashOf("Title", 1999, listOf(drama, noir))
        val sameFilm = identityHashOf("Title", 1999, listOf(Genre(id = 20, name = "Noir"), Genre(id = 10, name = "Drama")))

        assertThat(sameFilm).isEqualTo(hash)
        assertThat(hash).hasSize(64).matches("[0-9a-f]+")
    }

    @Test
    fun `should tell films apart by title, year and genres`() {
        val hash = identityHashOf("Title", 1999, listOf(drama))

        assertThat(identityHashOf("Other title", 1999, listOf(drama))).isNotEqualTo(hash)
        assertThat(identityHashOf("Title", 2000, listOf(drama))).isNotEqualTo(hash)
        assertThat(identityHashOf("Title", 1999, listOf(drama, noir))).isNotEqualTo(hash)
        assertThat(identityHashOf("Title", 1999, emptyList())).isNotEqualTo(hash)
    }
}
