package pl.ciruk.filmbag.film

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*
import jakarta.persistence.*
import jakarta.persistence.CascadeType.*
import jakarta.persistence.GenerationType.SEQUENCE
import org.hibernate.annotations.TimeZoneStorage
import org.hibernate.annotations.TimeZoneStorageType

@Entity
data class Film(
    @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(columnDefinition = "DATETIME(6)") val created: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS),
    val title: String,
    val year: Int,
    val plot: String? = null,
    val link: String,
    val poster: String? = null,
    @Column(columnDefinition = "DECIMAL(24,18)") var score: Double,

    @OneToMany(cascade = [ALL], mappedBy = "film", orphanRemoval = true)
    val scores: MutableSet<Score> = mutableSetOf(),

    @ManyToMany
    @Fetch(FetchMode.JOIN)
    @JoinTable(
        name = "film_genres",
        joinColumns = [JoinColumn(name = "film_id")],
        inverseJoinColumns = [JoinColumn(name = "genres_id")]
    )
    val genres: Set<Genre> = mutableSetOf(),

    @Column(name = "identity_hash", columnDefinition = "CHAR(64)")
    val identityHash: String = identityHashOf(title, year, genres)
) {
    /** @return false if the film already has an equal score */
    fun addScore(grade: Double, quantity: Long, type: ScoreType, url: String?): Boolean {
        return scores.add(Score(grade = grade, quantity = quantity, url = url, type = type, film = this))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Film

        if (id != other.id) return false
        if (identityHash != other.identityHash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        // Null for films stored before the column existed, until the startup job fills it
        result = 31 * result + Objects.hashCode(identityHash)
        return result
    }

    override fun toString(): String {
        return "Film(id=$id, title='$title', year=$year)"
    }
}

/** Films with the same title, year and genre names are the same film. */
fun identityHashOf(title: String, year: Int, genres: Collection<Genre>): String {
    val identity = (listOf(title, year.toString()) + genres.map { it.name }.sorted())
        .joinToString(UNIT_SEPARATOR)
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
    return HexFormat.of().formatHex(digest)
}

private const val UNIT_SEPARATOR = "\u001F"

@Entity
data class Score(
    @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
    @Enumerated(EnumType.STRING) @Column(columnDefinition = "VARCHAR(32)") val type: ScoreType,
    @Column(columnDefinition = "DECIMAL(24,18)") val grade: Double,
    val quantity: Long,
    val url: String? = null,
    @ManyToOne(cascade = [PERSIST, MERGE]) @JoinColumn(name = "film_id") val film: Film? = null
) {
    // Equal scores of one film are the same score; the id is left out so the hash stays stable on persist
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Score

        return type == other.type
                && grade == other.grade
                && quantity == other.quantity
                && url == other.url
    }

    override fun hashCode(): Int = Objects.hash(type, grade, quantity, url)
}

@Entity
data class Genre(
    @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
    val name: String
)

enum class ScoreType {
    AMATEUR,
    CRITIC,
    UNKNOWN,
    ;
}
