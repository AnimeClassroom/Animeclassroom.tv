package animeclassroom.tv.parser.sources

import animeclassroom.tv.dataclasses.*
import animeclassroom.tv.parser.*
import animeclassroom.tv.extractors.FPlayer
import animeclassroom.tv.extractors.GogoCDN
import animeclassroom.tv.extractors.StreamSB
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI

class GogoAnime : Parser(){
    override val name = "GogoAnime"
    override val hostUrl = "https://gogoanime.fi/"
    override val author = "Saikou"

    private fun httpsIfy(text: String): String {
        return if(text.take(2)=="//") "https:$text"
        else text
    }

    override suspend fun search(mediaObj: Media): List<SearchResponse> {
        val queryData = if (mediaObj.title!!.english != null) mediaObj.title!!.english!! else mediaObj.title!!.romaji!!
        val query = sanitizeQuery(queryData)
        val responseArray = arrayListOf<SearchResponse>()
        val htmlResponse = HttpClient.newCall(
            Request.Builder().url("${hostUrl}search.html?keyword=$query").build()
        ).execute().body!!.string()

        Jsoup.parse(htmlResponse)
            .select(".last_episodes > ul > li div.img > a").forEach {
                val link = it.attr("href").toString().replace("/category/", "")
                val title = it.attr("title")
                val cover = it.select("img").attr("src")
                responseArray.add(SearchResponse(title,link,cover))
            }
        return responseArray
    }

    override suspend fun loadEpisodes(url: String): List<Episode> {
        val responseArray = mutableListOf<Episode>()
        val pageBody = Jsoup.parse(
            HttpClient.newCall(Request.Builder().url("${hostUrl}category/$url").build()).execute().body!!.string()
        )
        val lastEpisode = pageBody.select("ul#episode_page > li:last-child > a").attr("ep_end").toString()
        val animeId = pageBody.select("input#movie_id").attr("value").toString()

        val a = Jsoup.parse(
            HttpClient.newCall(Request.Builder().url("https://ajax.gogo-load.com/ajax/load-list-episode?ep_start=0&ep_end=$lastEpisode&id=$animeId").build())
                .execute().body!!.string()
        ).select("ul > li > a").reversed()

        a.forEach{
            val num = it.select(".name").text().replace("EP","").trim()
            responseArray.add(Episode(number = num,link = hostUrl+it.attr("href").trim()))
        }

        return responseArray
    }

    override suspend fun loadVideoServerLinks(url: String): MutableMap<String, String> {
        val returnMap = mutableMapOf<String,String>()

        Jsoup.parse(
            HttpClient.newCall(Request.Builder().url(url).build()).execute().body!!.string()
        ).select("div.anime_muti_link > ul > li").forEach {
                returnMap[it.select("a").text().replace("Choose this server", "")] =
                    httpsIfy(it.select("a").attr("data-video"))
        }

        return returnMap
    }

    override suspend fun extractVideoServer(url: String, serverName: String): VideoServer? {
         val domain = URI(url).host
         val extractor : Extractor?=when {
             "gogo" in domain -> GogoCDN()
             "sb" in domain ->  StreamSB()
             "fplayer" in domain -> FPlayer()
             "fembed" in domain -> FPlayer()
             else -> null
         }
         extractor?.getStreamLinks(serverName,url)?.apply{
             if (videos.isNotEmpty()) return this
         }
         return null
    }

}
