package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.DataLoader
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Service
@Path("/import")
class ImportResource(private val dataLoader: DataLoader) {
    @GET
    fun import(): Response {
        dataLoader.importAsync()
        return Response.accepted().build()
    }
}
