package pl.ciruk.filmbag.boundary

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import pl.ciruk.filmbag.journal.Journal
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.FilmStore
import pl.ciruk.filmbag.testFilm

class FilmControllerTest {
    private val filmStore = mock(FilmStore::class.java)
    private val journal = mock(Journal::class.java)
    private val controller = FilmController(mock(FilmService::class.java), filmStore, journal)

    @Test
    fun `should record stored films`() {
        val films = listOf(testFilm())

        controller.store(films)

        verify(journal).recordAsync(films)
    }

    @Test
    fun `should not record films that could not be stored`() {
        val films = listOf(testFilm())
        doThrow(IllegalStateException("Storing failed")).`when`(filmStore).storeAll(films)

        assertThatThrownBy { controller.store(films) }.hasMessage("Storing failed")

        verifyNoInteractions(journal)
    }
}
