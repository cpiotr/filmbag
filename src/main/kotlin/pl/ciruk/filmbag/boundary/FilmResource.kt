package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.FilmService
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Service
@Transactional
@Path("/films")
class FilmResource(private val requestAdapter: RequestAdapter, private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): List<FilmRequest> {
        return filmService.findAll()
                .map { requestAdapter.convertToRequest(it) }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(filmRequest: FilmRequest) {
        val film = requestAdapter.convertToFilm(filmRequest)
        filmService.store(film)
    }
}
