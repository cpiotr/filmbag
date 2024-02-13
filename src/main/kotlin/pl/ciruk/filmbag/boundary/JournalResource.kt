package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import jakarta.ws.rs.GET
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
    private val logger = KotlinLogging.logger {}

    @GET
    @Path("/replay")
    @Produces(MediaType.APPLICATION_JSON)
    fun replay(): Response {
        journal.replay()
            .forEach { safeStoreAll(it) }
        return Response.accepted().build()
    }

    @GET
    @Path("/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional(readOnly = true)
    fun snapshot(): Response {
        var page = 0
        val size = 100
        do {
            val pageOfFilms = filmService.find(page = page, pageSize = size).map { it.convertToRequest() }
            journal.recordAsSnapshot(pageOfFilms)
            page++
        } while (pageOfFilms.size == size)
        return Response.accepted(mapOf(Pair("journalEntries", page))).build()
    }

    private fun safeStoreAll(it: List<FilmRequest>) {
        try {
            requestProcessor.storeAll(it)
        } catch (e: Exception) {
            logger.error(e) { "Could not store list of films: $it" }
        }
    }
}
