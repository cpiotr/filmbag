package pl.ciruk.filmbag.boundary

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.request.DataLoader
import pl.ciruk.filmbag.request.ImportAlreadyRunningException
import java.io.IOException
import java.util.concurrent.CompletionException
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

    @POST
    @Path("/{offset}")
    @Produces(MediaType.APPLICATION_JSON)
    fun import(
            @Suspended asyncResponse: AsyncResponse,
            @PathParam("offset") offset: Int) {
        logger.info { "Importing from offset: $offset" }

        if (offset < 0) {
            asyncResponse.resume(textResponse(Response.Status.BAD_REQUEST, "offset must be at least 0"))
            return
        }

        val importResult = try {
            dataLoader.importAsync(offset)
        } catch (e: ImportAlreadyRunningException) {
            asyncResponse.resume(textResponse(Response.Status.CONFLICT, e.message!!))
            return
        }

        asyncResponse.register(ConnectionCallback {
            logger.info { "Connection cancelled" }
            dataLoader.cancelImport()
        })
        importResult.whenComplete { lastPage, error ->
            asyncResponse.resume(
                    if (error == null) Response.accepted(lastPage).build()
                    else failureResponse(error))
        }
    }

    @POST
    @Path("/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    fun cancel(): Response {
        logger.info { "Cancel import" }

        dataLoader.cancelImport()

        return Response.ok().build()
    }

    private fun failureResponse(error: Throwable): Response {
        return when (val cause = (error as? CompletionException)?.cause ?: error) {
            is IOException -> textResponse(Response.Status.BAD_GATEWAY, "Import failed: ${cause.message}")
            else -> textResponse(Response.Status.INTERNAL_SERVER_ERROR, "Import failed")
        }
    }

    private fun textResponse(status: Response.Status, message: String): Response {
        return Response.status(status).type(MediaType.TEXT_PLAIN).entity(message).build()
    }
}
