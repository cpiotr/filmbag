package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Service
@Transactional
@Path("/films")
class FilmResource(private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): List<FilmRequest> {
        return filmService.find(EmptyRange(), EmptyRange())
                .map { it.convertToRequest() }
    }
}

fun Film.convertToRequest(): FilmRequest {
    return FilmRequest(
            created = this.created,
            title = this.title,
            year = this.year,
            link = this.link,
            score = this.score,
            numberOfScores = this.scores.size,
            scores = this.scores.map { ScoreRequest(it.grade, it.quantity, it.url) }.toSet(),
            genres = this.genres.map { it.name }.toSet(),
            plot = this.plot,
            poster = this.poster
    )
}
