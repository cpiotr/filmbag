package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Service
@Path("/journal")
class JournalResource(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal) {
    @GET
    @Path("/replay")
    fun replay(): Response {
        journal.replay()
                .forEach { requestProcessor.storeAll(it) }
        return Response.accepted().build()
    }
}
