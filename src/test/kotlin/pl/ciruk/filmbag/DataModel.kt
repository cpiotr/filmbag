package pl.ciruk.filmbag

import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.Genre
import pl.ciruk.filmbag.film.Score
import java.util.*

fun testFilmRequest(year: Int = 1912) = FilmRequest(
        title = "Test title " + UUID.randomUUID(),
        year = year,
        score = 0.623,
        numberOfScores = 4,
        scores = setOf(
                ScoreRequest(0.7, 123456, url = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-542576"),
                ScoreRequest(0.8, 2345, url = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-5425761"),
                ScoreRequest(0.1, 12, url = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-54257623"),
                ScoreRequest(0.3, 355, url = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-542576123")),
        genres = setOf("Genre1", "Genre2", "Genre3"),
        link = "http://test/image.png",
        poster = "https://www.filmweb.pl/film/Narodziny+gwiazdy-2018-542576dsff",
        plot = "Historia najbardziej czarującego złodzieja w historii, który wcale nie miał ochoty na to, żeby przejść na emeryturę, z więzienia uciekał 30 razy, a rabując banki nigdy nie zapominał o byciu gentlemanem.")

fun testOtherFilmRequest(year: Int = 1999) = FilmRequest(
        title = "Other title" + UUID.randomUUID(),
        year = year,
        score = 0.87,
        numberOfScores = 1,
        scores = setOf(ScoreRequest(0.1, 10, url = "Test other url")),
        genres = setOf("Genre2", "Genre1", "Genre4"),
        link = "http://other/image.png")

fun testFilm(year: Int = 2013) = Film(
        title = "Test title" + UUID.randomUUID(),
        year = year,
        link = "http://link",
        score = 0.3141593,
        scores = mutableSetOf(Score(grade = 0.3, quantity = 500), Score(grade = 0.4, quantity = 100)),
        genres = mutableSetOf(Genre(name = "Thriller")))
