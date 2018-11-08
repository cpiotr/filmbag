package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.request.RequestProcessor
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Service
@Transactional
@Path("/films")
class FilmResource(private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): List<FilmRequest> {
        return filmService.findAll()
                .map { it.convertToRequest() }
    }
}

fun Film.convertToRequest(): FilmRequest {
    return FilmRequest(
            title = this.title,
            year = this.year,
            link = this.link,
            score = this.score,
            numberOfScores = this.scores.size,
            scores = this.scores.map { ScoreRequest(it.grade, it.quantity) },
            genres = this.genres.map { it.name },
            plot = this.plot,
            poster = this.poster
    )
}
