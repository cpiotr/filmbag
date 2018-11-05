package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Component
import pl.ciruk.filmbag.film.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Component
@Path("/films")
class FilmResource(val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): List<FilmRequest> {
        return filmService.findAll()
                .map { it.toRequest() }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(filmRequest: FilmRequest) {
        filmService.store(filmRequest.toFilm())
    }
}
