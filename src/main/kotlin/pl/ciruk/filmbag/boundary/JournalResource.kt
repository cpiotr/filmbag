package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.ReplayResult
import pl.ciruk.filmbag.request.RequestProcessor
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Service
@Path("/journal")
class JournalResource(
    private val requestProcessor: RequestProcessor,
    private val journal: Journal,
    private val filmService: FilmService
) {
    @POST
    @Path("/replay")
    @Produces(MediaType.APPLICATION_JSON)
    fun replay(): ReplayResult {
        return journal.replay(requestProcessor::storeAll)
    }

    @POST
    @Path("/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    fun snapshot(): Response {
        var entries = 0
        for (films in filmService.findAllInPages(SNAPSHOT_PAGE_SIZE)) {
            journal.record(films.map { it.convertToRequest() })
            entries++
        }
        return Response.accepted(mapOf("journalEntries" to entries)).build()
    }

    companion object {
        private const val SNAPSHOT_PAGE_SIZE = 100
    }
}
