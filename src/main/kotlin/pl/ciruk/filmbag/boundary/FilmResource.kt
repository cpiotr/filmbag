package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import java.math.BigDecimal
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

private val logger = KotlinLogging.logger {}

@Service
@Transactional
@Path("/films")
class FilmReadResource(private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(
            @QueryParam("yearFrom") yearFrom: Int = missingInt,
            @QueryParam("yearTo") yearTo: Int = missingInt,
            @QueryParam("scoreFrom") scoreFrom: BigDecimal = missingDecimal,
            @QueryParam("scoreTo") scoreTo: BigDecimal = missingDecimal,
            @QueryParam("page") page: Int = firstPage,
            @QueryParam("pageSize") pageSize: Int = defaultPageSize): List<FilmRequest> {
        logger.info { "Find page $page of size $pageSize" }

        val yearRange = createYearRange(yearFrom, yearTo)
        val scoreRange = createScoreRange(scoreFrom, scoreTo)

        return filmService.find(yearRange, scoreRange, page, pageSize)
                .map { it.convertToRequest() }
    }

    private fun createScoreRange(scoreFrom: BigDecimal, scoreTo: BigDecimal): Range<BigDecimal> {
        return when (Pair(scoreFrom, scoreTo)) {
            Pair(missingDecimal, missingDecimal) -> EmptyRange()
            Pair(scoreFrom, missingDecimal) -> LeftClosedRange(scoreFrom)
            Pair(missingDecimal, scoreTo) -> RightClosedRange(scoreTo)
            else -> ClosedRange(scoreFrom, scoreTo)
        }
    }

    private fun createYearRange(yearFrom: Int, yearTo: Int): Range<Int> {
        return when (Pair(yearFrom, yearTo)) {
            Pair(missingInt, missingInt) -> EmptyRange()
            Pair(yearFrom, missingInt) -> LeftClosedRange(yearFrom)
            Pair(missingInt, yearTo) -> RightClosedRange(yearTo)
            else -> ClosedRange(yearFrom, yearTo)
        }
    }

    companion object {
        const val missingInt = -1
        val missingDecimal = BigDecimal.valueOf(-1.0)!!
        const val firstPage = 0
        const val defaultPageSize = 10
    }
}

@Service
@Transactional
@Path("/films")
class FilmWriteResource(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal) {
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeIfAbsent(filmRequests: List<FilmRequest>) {
        logger.info { "Store ${filmRequests.size} films, if missing" }
        journal.recordAsync(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }
}
