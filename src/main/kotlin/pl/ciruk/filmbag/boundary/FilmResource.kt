package pl.ciruk.filmbag.boundary

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.FilmService
import java.lang.invoke.MethodHandles
import java.math.BigDecimal
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Service
@Transactional
@Path("/films")
class FilmResource(private val filmService: FilmService) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(
            @DefaultValue(missingInt.toString()) @QueryParam("yearFrom") yearFrom: Int,
            @DefaultValue(missingInt.toString()) @QueryParam("yearTo") yearTo: Int,
            @DefaultValue(missingDecimal.toString()) @QueryParam("scoreFrom") scoreFrom: BigDecimal,
            @DefaultValue(missingDecimal.toString()) @QueryParam("scoreTo") scoreTo: BigDecimal,
            @DefaultValue("0") @QueryParam("page") page: Int,
            @DefaultValue("10") @QueryParam("pageSize") pageSize: Int
    ): List<FilmRequest> {
        logger.info("Find $page page of size $pageSize")

        val yearRange = when (Pair(yearFrom, yearTo)) {
            Pair(missingInt, missingInt) -> EmptyRange()
            Pair(yearFrom, missingInt) -> LeftClosedRange(yearFrom)
            Pair(missingInt, yearTo) -> RightClosedRange(yearTo)
            else -> ClosedRange(yearFrom, yearTo)
        }
        val scoreRange = when (Pair(scoreFrom, scoreTo)) {
            Pair(missingDecimal.toBigDecimal(), missingDecimal.toBigDecimal()) -> EmptyRange()
            Pair(scoreFrom, missingDecimal.toBigDecimal()) -> LeftClosedRange(scoreFrom)
            Pair(missingDecimal.toBigDecimal(), scoreTo) -> RightClosedRange(scoreTo)
            else -> ClosedRange(scoreFrom, scoreTo)
        }

        return filmService.find(yearRange, scoreRange, page, pageSize)
                .map { it.convertToRequest() }
    }

    companion object {
        const val missingInt = -1
        const val missingDecimal = -1.0
    }
}
