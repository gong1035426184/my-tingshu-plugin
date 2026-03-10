package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.extensions.get
import com.github.eprendre.tingshu.model.Album
import com.github.eprendre.tingshu.model.Episode
import org.jsoup.Jsoup

object EighteenTS : TingShu {
    private const val baseUrl = "https://www.18ts.com"
    private const val userAgent = "Mozilla/5.0 (Linux; Android 10; SM-G960N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36"

    override fun getSourceId(): String = "18ts"
    override fun getSourceName(): String = "18听书网"

    // 搜索逻辑
    override fun search(keywords: String, page: Int): Pair<List<Album>, Int> {
        val url = "$baseUrl/search.php?searchword=${keywords}&page=$page"
        val doc = Jsoup.connect(url).userAgent(userAgent).get()
        val albums = doc.select(".list-item").map { element ->
            Album(
                name = element.select(".title").text(),
                coverUrl = element.select("img").attr("abs:src"),
                intro = element.select(".description").text(),
                detailUrl = element.select("a").attr("abs:href"),
                author = element.select(".author").text(),
                artist = ""
            )
        }
        return Pair(albums, page + 1) // 假设总有下一页，接口会自动处理
    }

    // 获取章节列表
    override fun getAlbumDetail(album: Album): List<Episode> {
        val doc = Jsoup.connect(album.detailUrl).userAgent(userAgent).get()
        return doc.select(".playlist a").map {
            Episode(
                name = it.text(),
                url = it.attr("abs:href")
            )
        }
    }

    // 获取音频直链（最关键一步）
    override fun getAudioUrl(episode: Episode): String {
        // 18ts 通常在播放页的 script 标签里含有 playdata
        val doc = Jsoup.connect(episode.url).userAgent(userAgent).header("Referer", baseUrl).get()
        val scriptContent = doc.select("script").filter { it.html().contains("now") }.firstOrNull()?.html() ?: ""
        
        // 简单的正则匹配音频地址（根据实际加密情况可能需要解密，这里提供通用逻辑）
        val regex = "url\":\"(.*?)\"".toRegex()
        val matchResult = regex.find(scriptContent)
        var audioUrl = matchResult?.groupValues?.get(1)?.replace("\\/", "/") ?: ""
        
        // 如果地址是相对路径，补全它
        if (audioUrl.startsWith("/")) {
            audioUrl = "$baseUrl$audioUrl"
        }
        return audioUrl
    }

    // 分类浏览（可选，如果不需要可以返回空列表）
    override fun getCategories(): List<Category> = emptyList()
    override fun getCategoryList(category: Category, page: Int): Pair<List<Album>, Int> = Pair(emptyList(), 1)
}
