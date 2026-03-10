package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.model.Album
import com.github.eprendre.tingshu.model.Category
import com.github.eprendre.tingshu.model.Episode
import org.jsoup.Jsoup

object EighteenTS : TingShu {
    private const val baseUrl = "https://www.18ts.org"
    private const val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    override fun getSourceId(): String = "eighteen_ts"

    override fun getSourceName(): String = "18有声"

    override fun getCategories(): List<Category> {
        return listOf(
            Category("连载", "/list/1.html"),
            Category("完结", "/list/2.html")
        )
    }

    override fun getCategoryList(category: Category, page: Int): Pair<List<Album>, Int> {
        val url = if (page <= 1) "$baseUrl${category.url}" else "$baseUrl${category.url.replace(".html", "_$page.html")}"
        return parseAlbumList(url)
    }

    override fun search(keywords: String, page: Int): Pair<List<Album>, Int> {
        val url = "$baseUrl/search.php?searchword=$keywords&page=$page"
        return parseAlbumList(url)
    }

    private fun parseAlbumList(url: String): Pair<List<Album>, Int> {
        val doc = Jsoup.connect(url).userAgent(userAgent).get()
        val albums = doc.select(".list-item").map { element ->
            Album(
                name = element.select(".title").text(),
                coverUrl = element.select("img").attr("src"),
                intro = element.select(".intro").text(),
                detailUrl = element.select("a").attr("href"),
                author = "",
                artist = ""
            )
        }
        return Pair(albums, 1)
    }

    override fun getAlbumDetail(album: Album): List<Episode> {
        val doc = Jsoup.connect("$baseUrl${album.detailUrl}").userAgent(userAgent).get()
        return doc.select(".playlist a").map { element ->
            Episode(name = element.text(), url = element.attr("href"))
        }
    }

    override fun getAudioUrl(episode: Episode): String {
        return "$baseUrl${episode.url}"
    }
}
            Pair(albums, page + 1)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), page)
        }
    }

    // 2. 获取章节列表体检：捕获所有 a 标签并校验
    override fun getAlbumDetail(album: Album): List<Episode> {
        return try {
            val doc = Jsoup.connect(album.detailUrl).userAgent(userAgent).timeout(10000).get()
            
            // 针对不同可能的网页结构做了多重兼容
            doc.select(".playlist li a, .chapter-list a, #play_list a").map {
                Episode(
                    name = it.text(),
                    url = it.attr("abs:href")
                )
            }.filter { it.url.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 3. 核心大考：音频直链提取逻辑
    override fun getAudioUrl(episode: Episode): String {
        return try {
            // 必须携带 Referer 头部，否则很多听书网站会返回 403 禁止访问
            val doc = Jsoup.connect(episode.url)
                .userAgent(userAgent)
                .header("Referer", baseUrl)
                .timeout(10000)
                .get()

            // 方案 A：直接从 HTML5 的 <audio> 或 <source> 标签中获取
            var audioUrl = doc.select("audio source, audio").attr("src")
            if (audioUrl.isNotBlank()) {
                return formatUrl(audioUrl)
            }

            // 方案 B：如果网站对音频进行了基础的 JS 变量隐藏（如 JSON 格式的 player_data）
            val scriptContent = doc.select("script").html()
            
            // 尝试匹配常见的音频后缀 (.mp3, .m4a) 的 URL
            val regex = """(http|https)://[a-zA-Z0-9.\-/_]+(?:\.mp3|\.m4a)[^"'\s]*""".toRegex()
            val matchResult = regex.find(scriptContent)
            if (matchResult != null) {
                return formatUrl(matchResult.value.replace("\\/", "/"))
            }

            // 方案 C：匹配特定的字段，例如 "url":"..."
            val urlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
            val urlMatch = urlRegex.find(scriptContent)
            if (urlMatch != null) {
                return formatUrl(urlMatch.groupValues[1].replace("\\/", "/"))
            }

            "" // 如果各种手段都找不到，返回空字符串，App 端会提示播放失败
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // 辅助方法：确保提取出的链接是合法的绝对路径
    private fun formatUrl(url: String): String {
        return if (url.startsWith("//")) {
            "https:$url"
        } else if (url.startsWith("/")) {
            "$baseUrl$url"
        } else {
            url
        }
    }

    // 可选：分类逻辑（如果不需要可以直接返回空）
    override fun getCategories(): List<Category> = emptyList()
    override fun getCategoryList(category: Category, page: Int): Pair<List<Album>, Int> = Pair(emptyList(), page)
}
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
