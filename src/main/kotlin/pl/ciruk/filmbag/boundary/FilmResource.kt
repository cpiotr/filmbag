package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Component
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.Genre
import pl.ciruk.filmbag.film.Score
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Component
@Path("/films")
class FilmResource(val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): List<FilmRequest> {
        return filmService.findAll()
                .map {
                    FilmRequest(
                            title = it.title,
                            year = it.year,
                            link = it.link,
                            score = it.score,
                            numberOfScores = it.scores.size,
                            scores = it.scores.map { ScoreRequest(it.grade, it.quantity) }.toList(),
                            genres = it.genres.map { it.name }.toList(),
                            plot = it.plot,
                            poster = it.poster
                    )
                }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(filmRequest: FilmRequest) {
        val scores = filmRequest.scores.map { Score(grade = it.grade, quantity = it.quantity) }.toSet()
        val genres = filmRequest.genres.map { Genre(name = it) }.toSet()
        val film = Film(
                title = filmRequest.title,
                year = filmRequest.year,
                link = filmRequest.link,
                score = filmRequest.score,
                scores = scores,
                genres = genres,
                plot = filmRequest.plot,
                poster = filmRequest.poster

        )
        filmService.store(film)
    }
}