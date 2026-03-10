import com.github.eprendre.tingshu.model.Book
import com.github.eprendre.tingshu.model.BookDetail
import com.github.eprendre.tingshu.model.Category
import com.github.eprendre.tingshu.model.Episode
import com.github.eprendre.tingshu.sources.TingShu // 这一行是解决 image_7822dc 报错的关键
import org.jsoup.Jsoup
import java.net.URLEncoder

object EighteenTS : TingShu() {
    private val baseUrl = "https://www.18ts.com"
    private val mobileUA = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/004.1"

    override fun getName(): String = "18听书网"
    override fun getUrl(): String = baseUrl
    override fun getSelectedCategory(): Category? = null

    override fun getCategories(): List<Category> {
        return listOf(
            Category("玄幻奇幻", "$baseUrl/list/1.html"),
            Category("武侠修真", "$baseUrl/list/2.html"),
            Category("都市言情", "$baseUrl/list/3.html"),
            Category("青春校园", "$baseUrl/list/4.html"),
            Category("穿越重生", "$baseUrl/list/5.html"),
            Category("恐怖灵异", "$baseUrl/list/6.html"),
            Category("网游竞技", "$baseUrl/list/7.html"),
            Category("科幻史诗", "$baseUrl/list/8.html")
        )
    }

    override fun search(keywords: String, page: Int): List<Book> {
        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "$baseUrl/search.asp?page=$page&searchword=$encodedKeywords"
        val doc = Jsoup.connect(url).userAgent(mobileUA).get()
        return doc.select(".list-ul li").map {
            val a = it.select("a").first()
            Book(
                name = a.text(),
                url = baseUrl + a.attr("href"),
                author = it.select(".author").text(),
                artist = "",
                sourceName = getName()
            ).apply {
                coverUrl = it.select("img").attr("src")
                intro = it.select(".intro").text()
            }
        }
    }

    override fun getBookDetail(book: Book): BookDetail {
        val doc = Jsoup.connect(book.url).userAgent(mobileUA).get()
        val episodes = doc.select(".playlist ul li a").map {
            Episode(it.text(), baseUrl + it.attr("href"))
        }
        return BookDetail(episodes)
    }

    override fun getAudioUrl(episodeUrl: String): String {
        val doc = Jsoup.connect(episodeUrl).userAgent(mobileUA).header("Referer", baseUrl).get()
        val iframePath = doc.select("#play").attr("src")
        if (iframePath.isEmpty()) return ""
        val fullIframeUrl = if (iframePath.startsWith("http")) iframePath else baseUrl + iframePath
        val iframeDoc = Jsoup.connect(fullIframeUrl).userAgent(mobileUA).header("Referer", episodeUrl).get()
        val html = iframeDoc.html()
        val regex = Regex("var (?:datas|url)\\s*=\\s*[\"\\[']+(.*?)[\"\\]']+")
        val match = regex.find(html)
        var audioUrl
