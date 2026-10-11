package com.stremlopo.app

import org.junit.Assert.*
import org.junit.Test

class CatalogLogicTest {
    private fun film(id: String,title: String,genre: String) = Film(id,title,"Sinopse",genre,2020,10,"poster","Créditos","CC BY 3.0",listOf(VideoSource("https://example.org/movie.mp4","mp4")))
    @Test fun startupCatalogueDoesNotNeedServer() {
        val initial=listOf(film("1","Sintel","Animação"),film("2","Tears of Steel","Ficção científica"))
        assertEquals(initial,mergeCatalog(initial,emptyList()))
    }
    @Test fun searchIgnoresAccentsAndFindsGenres() {
        val items=listOf(film("1","Sintel","Animação"),film("2","Tears of Steel","Ficção científica"))
        assertEquals(listOf(items[0]),filterFilms(items,"animacao","Todos"))
        assertEquals(listOf(items[1]),filterFilms(items,"steel","Todos"))
        assertTrue(filterFilms(items,"desconhecido","Todos").isEmpty())
    }
    @Test fun remoteCatalogueIsMergedWithoutDuplicateTitles() {
        val first=film("1","Sintel","Animação"); val updated=first.copy(synopsis="Atualizada")
        val second=film("2","Outro","Aventura")
        assertEquals(listOf(updated,second),mergeCatalog(listOf(first),listOf(updated,second)))
    }
}
