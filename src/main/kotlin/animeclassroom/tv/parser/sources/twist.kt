package animeclassroom.tv.parser.sources

import animeclassroom.tv.dataclasses.*
import animeclassroom.tv.parser.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class Twist : Parser(){
    val host = "twist.moe"
    override val name = "Twist"
    override val hostUrl = "https://api.$host"
    override val author = "Saikou"

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class JsonTwist(
        val title : String,
        val alt_title : String?,
        val mal_id : Int,
        val slug : TwistSlug,
    ){
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class TwistSlug(val slug : String)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TwistSource(val number: Int,val source: String)

    object DecodeTwistSources{
        private val secret = "267041df55ca2b36f2e322d05ee2c9cf".toByteArray()
        private fun base64decode(oriString:String): ByteArray {
            return Base64.getDecoder().decode(oriString)
        }

        private fun md5(input:ByteArray): ByteArray {
            return MessageDigest.getInstance("MD5").digest(input)
        }

        private fun generateKey(salt:ByteArray): ByteArray {
            var key = md5(secret +salt)
            var currentKey = key
            while (currentKey.size < 48){
                key = md5(key + secret + salt)
                currentKey += key
            }
            return currentKey
        }

        private fun decryptSourceUrl(decryptionKey:ByteArray, sourceUrl: String): String {
            val cipherData = base64decode(sourceUrl)
            val encrypted = cipherData.copyOfRange(16, cipherData.size)
            val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")

            Objects.requireNonNull(aesCBC).init(
                Cipher.DECRYPT_MODE, SecretKeySpec(
                decryptionKey.copyOfRange(0,32),
                "AES"),
                IvParameterSpec(decryptionKey.copyOfRange(32,decryptionKey.size))
            )
            val decryptedData = aesCBC!!.doFinal(encrypted)
            return String(decryptedData, StandardCharsets.UTF_8)
        }

        fun decryptSource(input:String): String {
            return decryptSourceUrl(generateKey(base64decode(input).copyOfRange(8,16)),input)
        }
    }

    override suspend fun search(mediaObj: Media): List<SearchResponse> {
        val allAnime = jacksonObjectMapper().readValue<List<JsonTwist>>(
            HttpClient.newCall(
                Request.Builder().url("$hostUrl/api/anime").build()
            ).execute().body!!.string()
        )
        val resObj = allAnime.find { it.mal_id == mediaObj.idMal } ?: return emptyList()

        return listOf(SearchResponse(resObj.title, "$hostUrl/api/anime/${resObj.slug.slug}/sources",null))
    }

    override suspend fun loadEpisodes(url: String): List<Episode> {
            val returnList = mutableListOf<Episode>()
            jacksonObjectMapper().readValue<List<TwistSource>>(
                HttpClient.newCall(
                    Request.Builder().url(url).build()
                ).execute().body!!.string()
            ).forEach {
                returnList.add(Episode(it.number.toString(),it.source))
            }
            return returnList
    }

    override suspend fun loadVideoServerLinks(url: String): MutableMap<String, String> { // url scheme = api/anime/<slug>/sources/<episode>
        val apiUrl = url.substringBeforeLast('/')
        val episode = url.substringAfterLast('/').toInt() - 1
        jacksonObjectMapper().readValue<List<TwistSource>>(
            HttpClient.newCall(
                Request.Builder().url(apiUrl).build()
            ).execute().body!!.string()
        )[episode].also {
            return mutableMapOf("Twist" to it.source)
        }
    }

    override suspend fun extractVideoServer(url: String, serverName: String): VideoServer? { // pass url as encrypted Source
        return VideoServer(
            serverName,
            listOf(Video("Twist","https://$host${DecodeTwistSources.decryptSource(url)}",false))
        )
    }

}