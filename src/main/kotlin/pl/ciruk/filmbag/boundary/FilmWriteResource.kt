package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.request.RequestProcessor
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
@Transactional
@Path("/films")
class FilmWriteResource(private val requestProcessor: RequestProcessor) {
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeIfAbsent(filmRequest: FilmRequest): Response {
        requestProcessor.store(filmRequest)
        return Response.accepted().build()
    }
}
