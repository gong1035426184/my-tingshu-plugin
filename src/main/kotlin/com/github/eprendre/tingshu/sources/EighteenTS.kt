package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.model.Album
import com.github.eprendre.tingshu.model.Category
import com.github.eprendre.tingshu.model.Episode
import org.jsoup.Jsoup
import java.net.URLEncoder

object EighteenTS : TingShu {
    private const val baseUrl = "https://www.18ts.com"
    private const val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36"

    override fun getSourceId(): String = "18ts"
    override fun getSourceName(): String = "18听书网"

    override fun search(keywords: String, page: Int): Pair<List<Album>, Int> {
        return try {
            val encodedKw = URLEncoder.encode(keywords, "UTF-8")
            val url = "$baseUrl/search.php?searchword=$encodedKw&page=$page"
            val doc = Jsoup.connect(url).userAgent(userAgent).timeout(10000).get()
            
            val albums = doc.select(".list-item, .book-li, .search-list li").map { element ->
                Album(
                    name = element.select(".title, h3 a, .book-name").text(),
                    coverUrl = element.select("img").attr("abs:src"),
                    intro = element.select(".description, .intro, .desc").text(),
                    detailUrl = element.select("a").first()?.attr("abs:href") ?: "",
                    author = element.select(".author, .演播, .播音").text(),
                    artist = ""
                )
            }.filter { it.name.isNotBlank() && it.detailUrl.isNotBlank() }
            
            Pair(albums, page + 1)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), page)
        }
    }

    override fun getAlbumDetail(album: Album): List<Episode> {
        return try {
            val doc = Jsoup.connect(album.detailUrl).userAgent(userAgent).timeout(10000).get()
            doc.select(".playlist li a, .chapter-list a, #play_list a").map {
                Episode(name = it.text(), url = it.attr("abs:href"))
            }.filter { it.url.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getAudioUrl(episode: Episode): String {
        return try {
            val doc = Jsoup.connect(episode.url)
                .userAgent(userAgent)
                .header("Referer", baseUrl)
                .timeout(10000)
                .get()

            val scriptContent = doc.select("script").html()
            val regex = """(http|https)://[a-zA-Z0-9.\-/_]+(?:\.mp3|\.m4a)[^"'\s]*""".toRegex()
            val matchResult = regex.find(scriptContent)
            
            if (matchResult != null) {
                matchResult.value.replace("\\/", "/")
            } else {
                doc.select("audio source, audio").attr("src")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    override fun getCategories(): List<Category> = emptyList()

    override fun getCategoryList(category: Category, page: Int): Pair<List<Album>, Int> = Pair(emptyList(), page)
} // <--- 确保这行是文件的最后一行
