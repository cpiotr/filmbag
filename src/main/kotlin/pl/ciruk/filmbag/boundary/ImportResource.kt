package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.function.runWithoutFallback
import pl.ciruk.filmbag.request.DataLoader
import javax.ws.rs.*
import javax.ws.rs.container.AsyncResponse
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

        dataLoader.importAsync(offset ?: 0)
                .thenAccept { asyncResponse.resume(Response.accepted(it).build()) }
                .exceptionally {
                    runWithoutFallback {
                        logger.error(it) { "Cannot import data at offset $offset" }
                        val response = Response.serverError().entity(it.message).build()
                        asyncResponse.resume(response)
                    }
                }
    }
}
