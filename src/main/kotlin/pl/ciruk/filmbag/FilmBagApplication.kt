package pl.ciruk.filmbag

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(scanBasePackages = [
    "pl.ciruk.filmbag.boundary",
    "pl.ciruk.filmbag.config",
    "pl.ciruk.filmbag.film",
    "pl.ciruk.filmbag.request"
])
class FilmBagApplication

fun main(args: Array<String>) {
    runApplication<FilmBagApplication>(*args)
}