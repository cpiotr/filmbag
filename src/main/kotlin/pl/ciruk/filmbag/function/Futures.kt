package pl.ciruk.filmbag.function

fun <T: Any?> runWithFallback(result: T, handler: () -> Unit): T {
    handler()
    return result
}

fun runWithoutFallback(handler: () -> Unit): Void? {
    return runWithFallback(null, handler)
}
