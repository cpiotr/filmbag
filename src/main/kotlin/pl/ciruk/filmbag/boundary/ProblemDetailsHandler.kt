package pl.ciruk.filmbag.boundary

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import pl.ciruk.filmbag.provider.FilmProviderException
import pl.ciruk.filmbag.provider.ImportAlreadyRunningException

/** Reports every error as RFC 9457 Problem Details (`application/problem+json`), without internals. */
@RestControllerAdvice
class ProblemDetailsHandler : ResponseEntityExceptionHandler() {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler
    fun invalidFilms(e: InvalidFilmsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some films cannot be stored; nothing was stored")
            .apply { setProperty("invalidFilms", e.invalidFilms) }

    @ExceptionHandler
    fun importAlreadyRunning(e: ImportAlreadyRunningException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message)

    @ExceptionHandler
    fun filmProviderFailed(e: FilmProviderException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Import failed: ${e.message}")

    @ExceptionHandler
    fun unexpected(e: Exception): ProblemDetail {
        log.error(e) { "Request failed" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "The request failed; see the application log")
    }
}
