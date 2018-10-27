package pl.ciruk.filmbag

import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Component
@Path("/films")
class FilmResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): Film {
        val film = Film(
                title = "Shawshank",
                year = 1994,
                link = "http://localhost:8080/",
                score = 0.9,
                scores = listOf(Score(0.6, 1), Score(1.0, 100))
        )
        return film
    }
}