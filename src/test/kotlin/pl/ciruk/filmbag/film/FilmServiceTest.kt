package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.boundary.ClosedRange
import pl.ciruk.filmbag.boundary.LeftClosedRange
import pl.ciruk.filmbag.boundary.RightClosedRange
import pl.ciruk.filmbag.integration.TestConfiguration
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = [FilmBagApplication::class, TestConfiguration::class])
internal class FilmServiceTest(
        @Autowired val requestProcessor: RequestProcessor,
        @Autowired val filmService: FilmService) {

    val firstFilm = testFilmRequest(2009, 0.9)
    val secondFilm = testFilmRequest(2011, 0.7)
    val thirdFilm = testOtherFilmRequest(2012, 0.6)

    @BeforeEach
    internal fun setUp() {
        requestProcessor.storeAll(listOf(firstFilm, secondFilm, thirdFilm))
    }

    @Test
    fun `should find stored films by closed year range`() {
        val films = filmService.find(year = ClosedRange(2011, 2012))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))
    }

    @Test
    fun `should find stored films by left closed year range`() {
        val films = filmService.find(year = LeftClosedRange(2011))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))
    }

    @Test
    fun `should find stored films by right closed year range`() {
        val films = filmService.find(year = RightClosedRange(2011))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(firstFilm.title, secondFilm.title))
    }

    @Test
    fun `should not find films created later than specified range`() {
        val films = filmService.find(year = RightClosedRange(2008))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun `should not find films created earlier than specified range`() {
        val films = filmService.find(year = LeftClosedRange(2020))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun `should not find films created outside specified range`() {
        val films = filmService.find(year = ClosedRange(2010, 2010))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun `should find stored films by closed score range`() {
        val films = filmService.find(score = ClosedRange(0.6.toBigDecimal(), 0.8999.toBigDecimal()))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))
    }

    @Test
    fun `should find stored films by left closed score range`() {
        val films = filmService.find(score = LeftClosedRange(0.7.toBigDecimal()))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(firstFilm.title, secondFilm.title))
    }

    @Test
    fun `should find stored films by right closed score range`() {
        val films = filmService.find(score = RightClosedRange(0.89999.toBigDecimal()))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))
    }

    @Test
    fun `should not find films with score greater than specified range`() {
        val films = filmService.find(score = RightClosedRange(0.5999.toBigDecimal()))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun `should not find films with score lower than specified range`() {
        val films = filmService.find(score = LeftClosedRange(0.999.toBigDecimal()))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun `should not find films with score outside specified range`() {
        val films = filmService.find(score = ClosedRange(0.8.toBigDecimal(), 0.8.toBigDecimal()))

        assertThat(films.map { it.title })
                .isEmpty()
    }
}
