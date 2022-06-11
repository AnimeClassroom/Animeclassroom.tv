package animeclassroom.tv.parser.sources

import animeclassroom.tv.dataclasses.*
import animeclassroom.tv.parser.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

class NineAnime : Parser() {
    data class VidstreamResponse(
        val success : Boolean,
        val media: Sources,
    ){
        data class Sources(
            val sources: List<FileURL>
        )
        data class FileURL(
            val file: String
        )
    }
    override val name = "NineAnime"
    override val hostUrl = "https://animekisa.in/" // :TROL:
    override val author = "Saikou"

    // defaults

    override suspend fun search(mediaObj: Media): List<SearchResponse> {
        val queryData = if (mediaObj.title!!.english != null) mediaObj.title!!.english!! else mediaObj.title!!.romaji!!
        val query = sanitizeQuery(queryData)
        var url = URLEncoder.encode(query, "utf-8")
        if (query.startsWith("$!")) {
            val a = query.replace("$!", "").split(" | ")
            url = URLEncoder.encode(a[0], "utf-8") + a[1]
        }

        val request = Request.Builder().url("${hostUrl}filter?keyword=$url").build()
        val response = HttpClient.newCall(request).execute().body!!.string()

        val responseArray = mutableListOf<SearchResponse>()
        Jsoup.parse(response)
            .select("#main-wrapper .film_list-wrap > .flw-item .film-poster").forEach {
                val link = it.select("a").attr("href")
                val title = it.select("img").attr("title")
                val cover = it.select("img").attr("data-src")
                responseArray.add(SearchResponse(title, link, cover))
            }

        return responseArray
    }

    override suspend fun loadEpisodes(url: String): List<Episode> {
        val slugName = Regex("""(?<=\/watch\/)([a-zA-Z0-9\-]+)(?=-episode-\d+)""").find(url)!!.value
        val request = Request.Builder().url("$hostUrl/anime/$slugName").build()

        val returnList = mutableListOf<Episode>()

        Jsoup.parse(HttpClient.newCall(request).execute().body!!.string())
            .select("#listEps-1-content>ul>li>a").forEach {
                returnList.add(
                    Episode(
                        number = Regex("""\d+(?=\/${'$'})""").find(it.attr("href"))!!.value,
                        link = it.attr("href"),
                        title = it.attr("title"),
                    )
                )
            }
        return returnList
    }

    override suspend fun loadVideoServerLinks(url: String): MutableMap<String, String> {
        val request = Request.Builder().url(url).build()
        val response = HttpClient.newCall(request).execute().body!!.string()
        val returnMap = mutableMapOf<String, String>()
        Jsoup.parse(response).select("#servers-list > ul > li > a").forEach {

            returnMap[it.text()] = it.attr("data-embed")
        }
        return returnMap
    }

    override suspend fun extractVideoServer(url: String, serverName: String): VideoServer? {

            val token = Regex("(?<=window.skey = )'.*?'").find(
                HttpClient.newCall(Request.Builder().url(url).header("Referer",hostUrl).build()).execute().body!!.string()
            )?.value?.trim('\'') //token to get the m3u8

            val json =
                HttpClient.newCall(Request.Builder()
                    .url("${url.replace("/e/", "/info/")}&skey=$token")
                    .header("referer", hostUrl)
                    .build()).execute().body!!.string()

            val jsonDeserialized = jacksonObjectMapper().readValue<VidstreamResponse>(json)

            if(!jsonDeserialized.success) return null

            val videoList = mutableListOf<Video>()
            jsonDeserialized.media.sources.forEach {
                videoList.add(Video(
                    name = "MultiQuality",
                    url = it.file,
                    isM3U8 = true
                ))
            }
            return VideoServer(
                name = serverName,
                videos = videoList,
                headers = mutableMapOf("referer" to hostUrl)
            )
    }
}