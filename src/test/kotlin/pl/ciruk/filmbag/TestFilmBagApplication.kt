package pl.ciruk.filmbag

import org.springframework.boot.fromApplication
import org.springframework.boot.with

/** Runs the application against MariaDB and Redis in containers: `./gradlew bootTestRun`. */
fun main(args: Array<String>) {
    fromApplication<FilmBagApplication>().with(TestcontainersConfiguration::class).run(*args)
}
