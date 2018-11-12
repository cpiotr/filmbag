package pl.ciruk.filmbag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import pl.ciruk.filmbag.request.DataLoader

@ExtendWith(SpringExtension::class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = [TestConfiguration::class, FilmBagApplication::class])
class RequestProcessorIntegrationTest(@Autowired val restTemplate: TestRestTemplate) {
    @Test
    fun `should get created films`() {
        val filmRequest = FilmRequest(
                title = "Test title",
                year = 1912,
                score = 0.623,
                numberOfScores = 2,
                scores = setOf(ScoreRequest(0.1, 2), ScoreRequest(0.2, 3)),
                genres = setOf("Genre1", "Genre2", "Genre3"),
                link = "http://test/image.png"
        )
        val otherFilmRequest = FilmRequest(
                title = "Other title",
                year = 1999,
                score = 0.87,
                numberOfScores = 1,
                scores = setOf(ScoreRequest(0.1, 10)),
                genres = setOf("Genre2", "Genre1", "Genre4"),
                link = "http://other/image.png"
        )

        restTemplate.put("/resources/films", listOf(filmRequest, otherFilmRequest))
        val arrayOfFilmRequests = restTemplate.getForObject("/resources/films", Array<FilmRequest>::class.java)

        assertThat(arrayOfFilmRequests).containsOnly(filmRequest, otherFilmRequest)
    }
}

@Configuration
class TestConfiguration {
    @Bean
    @Primary
    fun dataLoader(): DataLoader {
        return mock(DataLoader::class.java)
    }
}