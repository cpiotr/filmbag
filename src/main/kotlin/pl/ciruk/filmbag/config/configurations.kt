package pl.ciruk.filmbag.config

import mu.KLogger

fun KLogger.logConfiguration(name: String, value: Any) {
    this.info { "$name: <$value>" }
}
