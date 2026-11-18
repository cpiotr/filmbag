package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import pl.ciruk.filmbag.testFilm
import pl.ciruk.filmbag.testOtherFilm

@IntegrationTest
internal class GenreServiceTest(
        @Autowired val filmStore: FilmStore,
        @Autowired val genreService: GenreService,
        @Autowired val testData: TestData) {

    @BeforeEach
    fun deleteAllData() {
        testData.deleteAll()
    }

    val firstFilm = testFilm(2009)
    val secondFilm = testFilm(2011)
    val thirdFilm = testOtherFilm(2012)

    @Test
    fun `should not create duplicate genres`() {
        val expectedGenres = (firstFilm.genres + secondFilm.genres + thirdFilm.genres).toSet()
        filmStore.storeAll(listOf(firstFilm, secondFilm, thirdFilm))

        val genres = genreService.findAll()

        assertThat(genres.map { it.name })
                .containsExactlyElementsOf(expectedGenres)
    }
}
