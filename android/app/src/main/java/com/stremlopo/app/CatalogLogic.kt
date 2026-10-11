package com.stremlopo.app

import java.text.Normalizer

data class VideoSource(val url: String, val format: String)
data class Film(
    val id: String, val title: String, val synopsis: String, val genre: String,
    val year: Int, val duration: Int, val poster: String, val attribution: String,
    val license: String, val sources: List<VideoSource>, val backdrop: String = poster,
    val evidence: String = "", val kind: String = "movie"
)
fun mergeCatalog(bundled: List<Film>,remote: List<Film>): List<Film> =
    (bundled + remote).associateBy { it.id }.values.toList()
private fun normalize(value: String) = Normalizer.normalize(value,Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
fun filterFilms(films: List<Film>,query: String,genre: String): List<Film> {
    val q=normalize(query.trim())
    return films.filter { (genre=="Todos" || normalize(it.genre).contains(normalize(genre))) &&
        (q.isBlank() || normalize(it.title+" "+it.genre).contains(q)) }
}
