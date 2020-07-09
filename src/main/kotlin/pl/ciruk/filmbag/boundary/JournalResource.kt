package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
@Path("/journal")
class JournalResource(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal) {
    private val logger = KotlinLogging.logger {}

    @GET
    @Path("/replay")
    @Produces(MediaType.APPLICATION_JSON)
    fun replay(): Response {
        journal.replay()
                .forEach { safeStoreAll(it) }
        return Response.accepted().build()
    }

    private fun safeStoreAll(it: List<FilmRequest>) {
        try {
            requestProcessor.storeAll(it)
        } catch (e: Exception) {
            logger.error(e) { "Could not store list of films: $it" }
        }
    }
}
