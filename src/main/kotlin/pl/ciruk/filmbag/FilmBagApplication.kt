package pl.ciruk.filmbag

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FilmBagApplication

fun main(args: Array<String>) {
    runApplication<FilmBagApplication>(*args)
}
