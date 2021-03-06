package pl.ciruk.filmbag

import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import pl.ciruk.filmbag.film.ScoreType
import java.util.*

fun testFilmRequest(year: Int = 1912, score: Double = 0.623) = FilmRequest(
        title = "Test title " + UUID.randomUUID(),
        year = year,
        score = score,
        numberOfScores = 4,
        scores = setOf(
                testScoreRequest(0.7, 123456, type = ScoreType.UNKNOWN),
                testScoreRequest(0.8, 2345, type = ScoreType.AMATEUR),
                testScoreRequest(0.1, 12, type = ScoreType.CRITIC),
                testScoreRequest(0.3, 355)),
        genres = setOf("Genre1", "Genre2", "Genre3"),
        link = "http://test/image.png",
        poster = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-542576dsff",
        plot = "Historia najbardziej czarującego złodzieja w historii, który wcale nie miał ochoty na to.")


fun testOtherFilmRequest(year: Int = 1999, score: Double = 0.87) = FilmRequest(
        title = "Other title" + UUID.randomUUID(),
        year = year,
        score = score,
        numberOfScores = 1,
        scores = setOf(ScoreRequest(0.1, 10, url = "Test other url")),
        genres = setOf("Genre2", "Genre1", "Genre4"),
        link = "http://other/image.png")

fun testScoreRequest(
        grade: Double = 0.8,
        quantity: Long = 1234,
        url: String = "https://test-url/film/score",
        type: ScoreType = ScoreType.CRITIC) = ScoreRequest(grade, quantity, url = url, type = type.name)
