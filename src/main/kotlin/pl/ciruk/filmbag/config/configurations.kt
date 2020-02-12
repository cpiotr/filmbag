package pl.ciruk.filmbag.config

import mu.KLogger
import mu.KotlinLogging

fun KLogger.logConfiguration(name: String, value: Any) {
    this.info {  "$name: <$value>" }
}
