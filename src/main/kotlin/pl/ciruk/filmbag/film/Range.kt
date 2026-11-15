package pl.ciruk.filmbag.film

/** Inclusive bounds on a film property used to filter films. */
sealed class Range<in T : Comparable<T>>

data class AtLeast<T : Comparable<T>>(val from: T) : Range<T>() {
    override fun toString() = "<$from, ∞)"
}

data class AtMost<T : Comparable<T>>(val to: T) : Range<T>() {
    override fun toString() = "(-∞, $to>"
}

data class Between<T : Comparable<T>>(val from: T, val to: T) : Range<T>() {
    override fun toString() = "<$from, $to>"
}

/** No bounds: matches every film. */
class Unbounded<T : Comparable<T>> : Range<T>() {
    override fun toString() = "(-∞, ∞)"
}

fun <T : Comparable<T>> rangeOf(from: T?, to: T?): Range<T> {
    return when {
        from != null && to != null -> Between(from, to)
        from != null -> AtLeast(from)
        to != null -> AtMost(to)
        else -> Unbounded()
    }
}
