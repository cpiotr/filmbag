package pl.ciruk.filmbag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
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
    fun `Should get created films`() {
        val genres = listOf("Genre1", "Genre2", "Genre3")
        val title = "Test title"
        val year = 1912
        val score = 0.623
        val numberOfScores = 2
        val link = "http://test/image.png"
        val filmRequest = FilmRequest(
                title = title,
                year = year,
                score = score,
                numberOfScores = numberOfScores,
                scores = listOf(ScoreRequest(0.1, 2), ScoreRequest(0.2, 3)),
                genres = genres,
                link = link
        )
        restTemplate.put("/resources/films", filmRequest)
        val entity = restTemplate.getForEntity<List<Map<String, Any>>>("/resources/films")
        assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(entity.body).hasSize(1)
        val firstResponseObject = entity.body?.first()
        assertThat(firstResponseObject)
                .containsEntry("title", title)
                .containsEntry("year", year)
                .containsEntry("score", score)
                .containsEntry("numberOfScores", numberOfScores)
                .containsEntry("link", link)
                .containsEntry("genres", genres)

        val listOfScores = firstResponseObject?.get("scores") as List<Map<String, Any>>
        assertThat(listOfScores)
                .containsOnly(
                        mapOf(Pair("grade", 0.1), Pair("quantity", 2)),
                        mapOf(Pair("grade", 0.2), Pair("quantity", 3))
                )
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