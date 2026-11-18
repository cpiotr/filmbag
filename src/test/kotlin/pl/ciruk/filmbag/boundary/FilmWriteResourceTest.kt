package pl.ciruk.filmbag.boundary

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.testFilmRequest

class FilmWriteResourceTest {
    private val requestProcessor = mock(RequestProcessor::class.java)
    private val journal = mock(Journal::class.java)
    private val resource = FilmWriteResource(requestProcessor, journal)

    @Test
    fun `should record stored films`() {
        val films = listOf(testFilmRequest())

        resource.storeOrUpdateScores(films)

        verify(journal).recordAsync(films)
    }

    @Test
    fun `should not record films that could not be stored`() {
        val films = listOf(testFilmRequest())
        doThrow(IllegalStateException("Storing failed")).`when`(requestProcessor).storeAll(films)

        assertThatThrownBy { resource.storeOrUpdateScores(films) }.hasMessage("Storing failed")

        verifyNoInteractions(journal)
    }
}
