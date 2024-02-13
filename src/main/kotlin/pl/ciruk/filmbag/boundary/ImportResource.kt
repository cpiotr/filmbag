package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.DataLoader
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.ws.rs.*
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.ConnectionCallback
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Service
@Path("/import")
class ImportResource(private val dataLoader: DataLoader) {
    private val logger = KotlinLogging.logger {}

    private val threadPool = Executors.newSingleThreadExecutor()

    @GET
    @Path("/{offset}")
    @Produces(MediaType.APPLICATION_JSON)
    fun import(
            @Suspended asyncResponse: AsyncResponse,
            @PathParam("offset") @DefaultValue("0") offset: Int?) {
        logger.info { "Importing from offset: $offset" }

        asyncResponse.register(ConnectionCallback() {
            logger.info { "Connection cancelled" }
            dataLoader.cancelImport()
        })
        try {
            val lastPage = dataLoader.importAsync(offset ?: 0)
                    .join()
            asyncResponse.resume(Response.accepted(lastPage).build())
        } catch (it: Exception) {
            logger.error(it) { "Cannot import data at offset $offset" }
            val response = Response.serverError().entity(it.message).build()
            asyncResponse.resume(response)
        }
    }

    @GET
    @Path("/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    fun cancel(): Response {
        logger.info { "Cancel import" }

        dataLoader.cancelImport()

        return Response.ok().build()
    }
}
