package pl.ciruk.filmbag.boundary

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.FilmStore
import pl.ciruk.filmbag.film.toFilmData
import pl.ciruk.filmbag.journal.Journal
import pl.ciruk.filmbag.journal.ReplayResult

@Service
@Path("/journal")
class JournalResource(
    private val filmStore: FilmStore,
    private val journal: Journal,
    private val filmService: FilmService
) {
    @POST
    @Path("/replay")
    @Produces(MediaType.APPLICATION_JSON)
    fun replay(): ReplayResult {
        return journal.replay(filmStore::storeAll)
    }

    @POST
    @Path("/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    fun snapshot(): Response {
        var entries = 0
        for (films in filmService.findAllInPages(SNAPSHOT_PAGE_SIZE)) {
            journal.record(films.map { it.toFilmData() })
            entries++
        }
        return Response.accepted(mapOf("journalEntries" to entries)).build()
    }

    companion object {
        private const val SNAPSHOT_PAGE_SIZE = 100
    }
}
