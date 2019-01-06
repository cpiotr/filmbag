package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.request.Journal
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
@Transactional
@Path("/films")
class FilmWriteResource(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal) {
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeIfAbsent(filmRequests: List<FilmRequest>): Response {
        journal.record(filmRequests)
        requestProcessor.storeAll(filmRequests)
        return Response.accepted().build()
    }
}
