package pl.ciruk.filmbag.film

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.SEQUENCE
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.TimeZoneStorage
import org.hibernate.annotations.TimeZoneStorageType
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.Objects

/** Films with the same title, year and genre names are one film; [identityHash] identifies it. */
@Entity
class Film(
    @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(columnDefinition = "DATETIME(6)") val created: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS),
    val title: String,
    val year: Int,
    val plot: String? = null,
    val link: String,
    val poster: String? = null,
    @Column(columnDefinition = "DECIMAL(24,18)") var score: Double,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "film", orphanRemoval = true)
    val scores: MutableSet<Score> = mutableSetOf(),

    @ManyToMany
    @JoinTable(
        name = "film_genres",
        joinColumns = [JoinColumn(name = "film_id")],
        inverseJoinColumns = [JoinColumn(name = "genres_id")]
    )
    val genres: Set<Genre> = mutableSetOf(),

    @Column(name = "identity_hash", columnDefinition = "CHAR(64)")
    val identityHash: String = identityHashOf(title, year, genres.map { it.name })
) {
    /** @return false if the film already has an equal score */
    fun addScore(grade: Double, quantity: Long, type: ScoreType, url: String?): Boolean {
        return scores.add(Score(grade = grade, quantity = quantity, url = url, type = type, film = this))
    }

    /** Takes the score and the scores of [other], a newer copy of this film. @return false if nothing changed */
    fun updateScoresFrom(other: Film): Boolean {
        val scoreChanged = score != other.score
        score = other.score
        val scoresAdded = other.scores.map { addScore(it.grade, it.quantity, it.type, it.url) }.any { it }
        return scoreChanged || scoresAdded
    }

    override fun equals(other: Any?) = other is Film && identityHash == other.identityHash

    override fun hashCode() = identityHash.hashCode()

    override fun toString() = "Film(id=$id, title='$title', year=$year)"
}

fun identityHashOf(title: String, year: Int, genreNames: Collection<String>): String {
    val identity = (listOf(title, year.toString()) + genreNames.sorted())
        .joinToString(UNIT_SEPARATOR)
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
    return HexFormat.of().formatHex(digest)
}

private const val UNIT_SEPARATOR = "\u001F"

/** Scores of one film with equal type, grade, quantity and url are one score. */
@Entity
class Score(
    @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
    @Enumerated(EnumType.STRING) @Column(columnDefinition = "VARCHAR(32)") val type: ScoreType,
    @Column(columnDefinition = "DECIMAL(24,18)") val grade: Double,
    val quantity: Long,
    val url: String? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "film_id") val film: Film
) {
    // The id is left out, so the hash stays the same when the score is persisted
    override fun equals(other: Any?) = other is Score
            && type == other.type
            && grade == other.grade
            && quantity == other.quantity
            && url == other.url

    override fun hashCode() = Objects.hash(type, grade, quantity, url)

    override fun toString() = "Score(type=$type, grade=$grade, quantity=$quantity, url=$url)"
}

/** Genre names are unique. */
@Entity
class Genre(
    @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
    val name: String
) {
    override fun equals(other: Any?) = other is Genre && name == other.name

    override fun hashCode() = name.hashCode()

    override fun toString() = "Genre(name='$name')"
}

enum class ScoreType {
    AMATEUR,
    CRITIC,
    UNKNOWN,
}
