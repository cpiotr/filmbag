package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.boundary.ClosedRange
import pl.ciruk.filmbag.boundary.LeftClosedRange
import pl.ciruk.filmbag.integration.TestConfiguration
import pl.ciruk.filmbag.testFilm

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = [FilmBagApplication::class, TestConfiguration::class])
internal class FilmServiceTest(@Autowired val filmService: FilmService) {

    @Test
    fun shouldStore() {
        val firstFilm = testFilm(2010)
        val secondFilm = testFilm(2011)
        val thirdFilm = testFilm(2012)
        filmService.storeAll(listOf(firstFilm, secondFilm, thirdFilm))

        val films = filmService.find(year = ClosedRange(2011, 2012))

        Assertions.assertThat(films.map { it::title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))

    }

    @Test
    fun shouldFind() {
    }
}
