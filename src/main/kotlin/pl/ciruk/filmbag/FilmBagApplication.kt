package pl.ciruk.filmbag

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FilmBagApplication{
}

fun main(args: Array<String>) {
    runApplication<FilmBagApplication>(*args)
}