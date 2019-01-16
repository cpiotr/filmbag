package pl.ciruk.filmbag.function

fun <T: Any?> logWithFallback(result: T, handler: () -> Unit): T {
    handler()
    return result
}

fun logWithoutFallback(handler: () -> Unit): Void? {
    return logWithFallback(null, handler)
}