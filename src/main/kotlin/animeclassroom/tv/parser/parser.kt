package animeclassroom.tv.parser

import animeclassroom.tv.dataclasses.*
import okhttp3.OkHttpClient

object HttpClient : OkHttpClient()

abstract class Extractor{
    abstract fun getStreamLinks(name:String, url: String): VideoServer
}

@Suppress("unused")
abstract class Parser{
    abstract val name : String
    abstract val hostUrl : String
    abstract val author : String

    open val isAnime = true
    open val language = "en"
    open val isDubbed = false
    val headers = mutableMapOf<String,String>()

    /**
     *  Search for Anime, returns a List of Responses, having name, url & other metadata
     **/
    abstract suspend fun search(mediaObj: Media) : List<SearchResponse>

    /**
     * Takes a SearchResponse.url as argument & gives total episode present on the server.
     */
    abstract suspend fun loadEpisodes(url: String) : List<Episode>

    /**
     * Most of the time takes Episode.link as parameter, but you can use anything else if needed
     * This returns a Map of "Video Server's Name" & "Link/Data" of all the Extractor VideoServers, which can be further used by loadVideoServers() & loadSingleVideoServer()
     */
    abstract suspend fun loadVideoServerLinks (url:String) : MutableMap<String,String>

    /**
     * Takes an url or any other data as an argument & returns VideoServer with all Video Qualities of that particular server.
     * This is where you should use External Extractors.
     */
    abstract suspend fun extractVideoServer(url:String, serverName:String) : VideoServer?

    //Example :
    //     val domain = URI(url).host
    //     val extractor : Extractor?=when {
    //         "gogo" in domain -> GogoCDN()
    //         "sb" in domain ->  StreamSB()
    //         "fplayer" in domain -> FPlayer()
    //         else -> null
    //     }
    //     extractor?.getStreamLinks(serverName,url)?.apply{
    //         if (videos.isNotEmpty()) return this
    //     }
    //     return null


    /**
     * This Function used when there "isn't" a default Server set by the user, or when user wants to switch the Server
     * Doesn't need to be overridden, if the parser is following the norm.
    */
    open suspend fun loadVideoServers(url:String, callback: (VideoServer) -> Unit) {
        loadVideoServerLinks(url).forEach{
            extractVideoServer(it.value,it.key)?.apply{ callback.invoke(this) }
        }
    }

    /**
     * This Function used when there "is" a default Server set by the user, only loads a Single Server for faster response.
     * Doesn't need to be overridden, if the parser is following the norm.
    */
    open suspend fun loadSingleVideoServer(serverName: String, url: String) : VideoServer? {
        loadVideoServerLinks(url).apply{
            if(containsKey(serverName)) return extractVideoServer(this[serverName]!!, serverName)
        }
        return null
    }

    open fun sanitizeQuery(query:String) : String{
        return Regex("\\W+").replace(query," ")
    }

    open var displayText = ""
    open var displayTextListener : ((String)->Unit)? = null

    /**
     * Used to send messages & errors to the User, a useful way to convey what's happening on currently being done & what was done.
     */
    fun setText(string:String){
        displayText = string
        displayTextListener?.invoke(displayText)
    }
}

data class SearchResponse(
    val name: String,
    val url: String,
    val coverUrl: String?,
    
    val otherNames: List<String> = listOf(),
    val totalEpisodes: Int? = null,
)

data class VideoServer(
    val name: String,
    val videos: List<Video>,
    val headers : MutableMap<String,String> = mutableMapOf(),
    val subtitles : List<Subtitle> = listOf(),
)

data class Video(
    val name: String,
    val url: String,
    val isM3U8: Boolean,
    val size : Long? = null,
    val extraNote:String? = null,
)

data class Subtitle(
    val language: String,
    val url: String,
)

data class Episode(
    val number: String,
    val link : String,
    
    val title : String?=null,
    val thumbnail : String?=null,
    val description : String?=null,
    val isFiller : Boolean = false,
)