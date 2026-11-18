package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    @Transactional(readOnly = true)
    fun snapshot(): Response {
        var page = 0
        val size = 100
        do {
            val pageOfFilms = filmService.find(page = page, pageSize = size).map { it.convertToRequest() }
            journal.record(pageOfFilms)
            page++
        } while (pageOfFilms.size == size)
        return Response.accepted(mapOf(Pair("journalEntries", page))).build()
    }
}
