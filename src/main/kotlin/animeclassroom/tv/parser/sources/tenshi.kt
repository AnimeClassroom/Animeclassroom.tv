package animeclassroom.tv.parser.sources

import animeclassroom.tv.dataclasses.*
import animeclassroom.tv.parser.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI

class Tenshi : Parser(){
    override val name = "Tenshi"
    override val hostUrl = "https://tenshi.moe"
    override val author = "Saikou"

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class SourceJs(
        val type:String,
        val sources : List<TenshiSources>,
        val poster : String?,
    ){
        data class TenshiSources(val src:String,val type: String,val size:Int)
    }

    override suspend fun search(mediaObj: Media): List<SearchResponse> {
        val queryData = if (mediaObj.title!!.english != null) mediaObj.title!!.english!! else mediaObj.title!!.romaji!!
        val query = sanitizeQuery(queryData)
        val responseArray = arrayListOf<SearchResponse>()

        val htmlResponse = HttpClient.newCall(
            Request.Builder().url("$hostUrl/anime?q=$query&s=vtt-d")
                .header("Cookie","__ddg1_=;__ddg2_=;loop-view=thumb").build()
        ).execute().body!!.string()

        Jsoup.parse(htmlResponse).
            select("ul.loop.anime-loop.thumb > li > a").forEach{
                SearchResponse(
                    name = it.attr("title"),
                    url = it.attr("abs:href"),
                    coverUrl = it.select(".image")[0].attr("src"),
                ).also { searchResponse -> responseArray.add(searchResponse)}
            }
        return responseArray
    }

    override suspend fun loadEpisodes(url: String): List<Episode> {
        val htmlResponse = HttpClient.newCall(
            Request.Builder().url(url)
                .header("Cookie","__ddg1_=;__ddg2_=;loop-view=thumb").build()
        ).execute().body!!.string()

        val returnList = mutableListOf<Episode>()

        Jsoup.parse(htmlResponse).select("ul.loop.episode-loop.thumb > li > a").forEach {
                Episode(
                    number = it.select("div.episode-slug").text().replace("Episode ",""),
                    title = it.select("div.episode-title").text(),
                    link = it.attr("href"),
                    thumbnail = it.select("img.image").attr("src"),
                    description = it.attr("data-content")
                ).also {episode -> returnList.add(episode) }
        }
        return returnList
    }

    override suspend fun loadVideoServerLinks(url: String): MutableMap<String, String> {
        val htmlResponse = HttpClient.newCall(
            Request.Builder().url(url)
                .header("Cookie","__ddg1_=;__ddg2_=;loop-view=thumb").build()
        ).execute().body!!.string()

        val returnMap = mutableMapOf<String,String>()

        Jsoup.parse(htmlResponse).select("ul.dropdown-menu > li > a").forEach {
            returnMap[it.select("span").attr("title")] = it.attr("href")
        }

        return returnMap
    }

    override suspend fun extractVideoServer(url: String, serverName: String): VideoServer? {
        val query = URI(url).query
        val embedUrl = "https://tenshi.moe/embed?$query"
        val htmlSourceJson = HttpClient.newCall(
            Request.Builder().url(embedUrl)
                .header("Cookie","__ddg1_=;__ddg2_=;loop-view=thumb")
                .header("Referer",url.replace(query,"")).build()
        ).execute().body!!.string().substringAfter("player.source = ").substringBefore(';')

        val returnVideoList = mutableListOf<Video>()

        jacksonMapperBuilder()
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA).build()

        .readValue<SourceJs>(htmlSourceJson).sources.forEach {
            Video(
                name = it.size.toString(),
                url = it.src,
                isM3U8 = false,
            ).also {ep -> returnVideoList.add(ep) }
        }
        return VideoServer(serverName,returnVideoList, mutableMapOf("Cookies" to "__ddg1_=; __ddg2_=;"))
    }
}