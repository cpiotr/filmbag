package pl.ciruk.filmbag.film

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity
data class Film(
        @Id @GeneratedValue val id: Long? = null,
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        @OneToMany val scores: Set<Score> = setOf(),
        @OneToMany val genres: Set<Genre> = setOf())

@Entity
data class Score (
        @Id @GeneratedValue val id: Long? = null,
        val grade: Double,
        val quantity: Long)

@Entity
data class Genre (
        @Id @GeneratedValue val id: Long? = null,
        val name: String)