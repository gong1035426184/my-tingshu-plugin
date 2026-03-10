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
