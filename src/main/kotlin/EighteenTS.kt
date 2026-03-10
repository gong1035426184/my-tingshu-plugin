import com.github.eprendre.tingshu.model.Book
import com.github.eprendre.tingshu.model.BookDetail
import com.github.eprendre.tingshu.model.Category
import com.github.eprendre.tingshu.model.Episode
import com.github.eprendre.tingshu.sources.TingShu // 必须有这一行
import org.jsoup.Jsoup
import java.net.URLEncoder

object EighteenTS : TingShu() {
    // 剩下的代码保持不变，但要确保最后只有一个 }
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
} // 确保最后只有一个这个大括号
