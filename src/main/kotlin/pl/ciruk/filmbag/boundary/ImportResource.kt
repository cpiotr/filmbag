package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.DataLoader
import java.util.concurrent.Executors
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Service
@Path("/import")
class ImportResource(private val dataLoader: DataLoader) {
    private val threadPool = Executors.newSingleThreadExecutor()

    @GET
    fun import(): Response {
        threadPool.execute(dataLoader::import)
        return Response.accepted().build()
    }
}
