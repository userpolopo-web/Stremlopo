package com.stremlopo.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class VideoSource(val url: String, val format: String)
data class Film(val id: String, val title: String, val synopsis: String, val genre: String, val year: Int, val duration: Int, val poster: String, val attribution: String, val license: String, val sources: List<VideoSource>)
object Api {
    private fun connection(base: String, path: String): HttpURLConnection {
        val url = URL(base.trimEnd('/') + path)
        require(url.protocol == "https" && url.host.isNotBlank()) { "Configure um servidor HTTPS." }
        return (url.openConnection() as HttpURLConnection).apply { connectTimeout=10000; readTimeout=10000; instanceFollowRedirects=false }
    }
    suspend fun catalog(base: String): List<Film> = withContext(Dispatchers.IO) {
        val c=connection(base,"/api/catalog")
        try {
            check(c.responseCode == 200) { "Servidor indisponível (${c.responseCode})." }
            val items=JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            (0 until items.length()).map { i ->
                val o=items.getJSONObject(i); val s=o.getJSONArray("sources")
                Film(o.getString("id"),o.getString("title"),o.optString("synopsis"),o.optString("genre"),o.optInt("year"),o.optInt("duration"),o.optString("poster"),o.optString("attribution"),o.optString("license"),(0 until s.length()).map { j -> val v=s.getJSONObject(j); VideoSource(v.getString("url"),v.getString("format")) })
            }
        } finally { c.disconnect() }
    }
    suspend fun report(base: String,id: String,reason: String) = withContext(Dispatchers.IO) {
        val c=connection(base,"/api/reports")
        try { c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Content-Type","application/json")
            c.outputStream.use { it.write(JSONObject().put("content_id",id).put("reason",reason).toString().toByteArray()) }
            check(c.responseCode==201) { "Não foi possível enviar a denúncia." }
        } finally { c.disconnect() }
    }
}
