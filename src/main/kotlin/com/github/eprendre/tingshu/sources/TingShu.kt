package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.model.Album
import com.github.eprendre.tingshu.model.Category
import com.github.eprendre.tingshu.model.Episode

interface TingShu {
    fun getSourceId(): String
    fun getSourceName(): String
    fun getCategories(): List<Category>
    fun getCategoryList(category: Category, page: Int): Pair<List<Album>, Int>
    fun search(keywords: String, page: Int): Pair<List<Album>, Int>
    fun getAlbumDetail(album: Album): List<Episode>
    fun getAudioUrl(episode: Episode): String
}
