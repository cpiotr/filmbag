package pl.ciruk.filmbag

import pl.ciruk.filmbag.film.FilmData
import pl.ciruk.filmbag.film.ScoreData
import pl.ciruk.filmbag.film.ScoreType
import java.util.*

fun testFilm(year: Int = 1912, score: Double = 0.623) = FilmData(
        title = "Test title " + UUID.randomUUID(),
        year = year,
        score = score,
        numberOfScores = 4,
        scores = setOf(
                testScore(0.7, 123456, type = ScoreType.UNKNOWN),
                testScore(0.8, 2345, type = ScoreType.AMATEUR),
                testScore(0.1, 12, type = ScoreType.CRITIC),
                testScore(0.3, 355)),
        genres = setOf("Genre1", "Genre2", "Genre3"),
        link = "http://test/image.png",
        poster = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-542576dsff",
        plot = "Historia najbardziej czarującego złodzieja w historii, który wcale nie miał ochoty na to.")


fun testOtherFilm(year: Int = 1999, score: Double = 0.87) = FilmData(
        title = "Other title" + UUID.randomUUID(),
        year = year,
        score = score,
        numberOfScores = 1,
        scores = setOf(ScoreData(0.1, 10, url = "Test other url")),
        genres = setOf("Genre2", "Genre1", "Genre4"),
        link = "http://other/image.png")

fun testScore(
        grade: Double = 0.8,
        quantity: Long = 1234,
        url: String = "https://test-url/film/score",
        type: ScoreType = ScoreType.CRITIC) = ScoreData(grade, quantity, url = url, type = type.name)
