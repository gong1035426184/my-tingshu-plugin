package sources_by_myself

import com.github.eprendre.tingshu.model.Book
import com.github.eprendre.tingshu.model.BookDetail
import com.github.eprendre.tingshu.model.Category
import com.github.eprendre.tingshu.model.Episode
import com.github.eprendre.tingshu.sources.TingShu
import org.jsoup.Jsoup
import java.net.URLEncoder

object EighteenTS : TingShu() {
    override fun getName(): String = "18听书网"
    override fun getUrl(): String = "https://www.18ts.com"
    
    private val mobileUA = "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36"

    override fun search(keywords: String, page: Int): Pair<List<Book>, Int> {
        val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
        val url = "$baseUrl/search.html?searchword=$encodedKeywords&page=$page"
        val doc = Jsoup.connect(url).userAgent(mobileUA).get()
        
        val books = doc.select(".list-ul li").map { element ->
            Book(
                title = element.select("h2").text(),
                author = element.select(".author").text(),
                coverUrl = element.select("img").let { 
                    val src = it.attr("data-original")
                    if (src.isNullOrEmpty()) it.attr("src") else src 
                },
                bookUrl = baseUrl + element.select("a").attr("href"),
                sourceId = getName()
            )
        }
        return Pair(books, page + 1)
    }

    override fun getCategoryList(): List<Category> {
        val doc = Jsoup.connect(baseUrl).userAgent(mobileUA).get()
        return doc.select(".nav-ol li.nav-li a").mapNotNull { element ->
            val title = element.text()
            val url = element.attr("href")
            if (title == "首页" || title == "排行榜" || title == "听单") null 
            else Category(title, baseUrl + url)
        }
    }

    override fun getCategory(categoryUrl: String, page: Int): Pair<List<Book>, Int> {
        val targetUrl = if (page == 1) categoryUrl else categoryUrl.replace(".html", "/$page.html")
        val doc = Jsoup.connect(targetUrl).userAgent(mobileUA).get()
        
        val books = doc.select(".list-ul li").map { element ->
            Book(
                title = element.select("h2").text(),
                author = element.select(".author").text(),
                coverUrl = element.select("img").let { 
                    val src = it.attr("data-original")
                    if (src.isNullOrEmpty()) it.attr("src") else src 
                },
                bookUrl = baseUrl + element.select("a").attr("href"),
                sourceId = getName()
            )
        }
        val hasNextPage = doc.select(".page a").any { it.text().contains("下一页") }
        return Pair(books, if (hasNextPage) page + 1 else page)
    }

    override fun getBookDetail(bookUrl: String): BookDetail {
        val doc = Jsoup.connect(bookUrl).userAgent(mobileUA).get()
        val intro = doc.select(".book-des").text()
        
        val allChaptersUrl = doc.select(".dirurl").attr("href")
        val targetDoc = if (allChaptersUrl.isNotEmpty()) {
            Jsoup.connect(baseUrl + allChaptersUrl).userAgent(mobileUA).get()
        } else {
            doc
        }

        val episodes = targetDoc.select(".playlist ul li a").map { element ->
            Episode(
                title = element.text(),
                url = baseUrl + element.attr("href")
            )
        }
        return BookDetail(episodes, intro)
    }

    override fun getAudioUrl(episodeUrl: String): String {
        val doc = Jsoup.connect(episodeUrl).userAgent(mobileUA).header("Referer", baseUrl).get()
        val iframePath = doc.select("#play").attr("src")
        if (iframePath.isEmpty()) return ""
        
        val fullIframeUrl = if (iframePath.startsWith("http")) iframePath else baseUrl + iframePath

        val iframeDoc = Jsoup.connect(fullIframeUrl)
            .userAgent(mobileUA)
            .header("Referer", episodeUrl)
            .get()
            
        val html = iframeDoc.html()
        val regex = Regex("var (?:datas|url)\\s*=\\s*[\"\\[']+(.*?)[\"\\]']+")
        val match = regex.find(html)
        
        var audioUrl = match?.groupValues?.get(1) ?: ""
        if (audioUrl.isNotEmpty() && !audioUrl.startsWith("http")) {
            audioUrl = baseUrl + audioUrl
        }
        return audioUrl
    }
}
