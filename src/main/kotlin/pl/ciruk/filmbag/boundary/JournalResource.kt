package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.request.RequestRecorder
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
@Path("/journal")
class JournalResource(private val requestRecorder: RequestRecorder) {
    @GET
    @Path("/replay")
    fun replay(): Response {
        requestRecorder.replay()
        return Response.accepted().build()
    }
}
