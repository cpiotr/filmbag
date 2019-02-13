package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.DataLoader
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.Response

@Service
@Path("/import")
class ImportResource(private val dataLoader: DataLoader) {
    @GET
    @Path("{offset}")
    fun import(@Suspended asyncResponse: AsyncResponse, @PathParam("offset") @DefaultValue("0") offset: Int?) {
        dataLoader.importAsync(offset ?: 0)
                .thenAccept { asyncResponse.resume(Response.accepted(it).build()) }
                .exceptionally {
                    asyncResponse.resume(Response.serverError().entity(it).build())
                    null
                }
    }
}
