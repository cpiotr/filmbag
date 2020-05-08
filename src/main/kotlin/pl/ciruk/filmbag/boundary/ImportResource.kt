package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.function.runWithoutFallback
import pl.ciruk.filmbag.request.DataLoader
import javax.ws.rs.*
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.ConnectionCallback
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
@Path("/import")
class ImportResource(private val dataLoader: DataLoader) {
    private val logger = KotlinLogging.logger {}

    @GET
    @Path("/{offset}")
    @Produces(MediaType.APPLICATION_JSON)
    fun import(
            @Suspended asyncResponse: AsyncResponse,
            @PathParam("offset") @DefaultValue("0") offset: Int?) {
        logger.info { "Importing from offset: $offset" }

        val future = dataLoader.importAsync(offset ?: 0)

        asyncResponse.register(ConnectionCallback() {
            logger.info { "Connection cancelled" }
            future.cancel(true)
        })

        try {
            asyncResponse.resume(Response.accepted(future::join).build())
        } catch (e: Exception) {
            logger.error(e) { "Cannot import data at offset $offset" }
            val response = Response.serverError().entity(e.message).build()
            asyncResponse.resume(response)
        }
    }
}
