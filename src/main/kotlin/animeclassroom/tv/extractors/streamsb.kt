package animeclassroom.tv.extractors

import animeclassroom.tv.parser.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request

class StreamSB : Extractor() {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SBJsonResponse(
        val stream_data: StreamData,
        val status_code:Int,
    ){
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class StreamData(
            val file:String,
            val backup : String
        )
    }

    override fun getStreamLinks(name:String, url: String): VideoServer {
        val hexArray = "0123456789ABCDEF".toCharArray()

        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF

                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }
        val id = url.substringAfter("e/").substringBefore(".html")
        val bytesToHex = bytesToHex(id.toByteArray())

        val headers = mutableMapOf("Referer" to "$url/","User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
        val source =
            HttpClient.newCall(
                Request.Builder().url("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/sb.txt").build()
            ).execute().body!!.string()

        val jsonLink = "$source/7361696b6f757c7c${bytesToHex}7c7c7361696b6f757c7c73747265616d7362/7361696b6f757c7c363136653639366436343663363136653639366436343663376337633631366536393664363436633631366536393664363436633763376336313665363936643634366336313665363936643634366337633763373337343732363536313664373336327c7c7361696b6f757c7c73747265616d7362"

        val deserializedJson =
            jacksonObjectMapper().readValue<SBJsonResponse>(
                HttpClient.newCall(Request.Builder().url(jsonLink)
                    .headers(headers.toHeaders())
                    .addHeader("watchsb","streamsb")
                    .build()).execute().body!!.string()
            )

        return VideoServer(
            name,
            listOf(
                Video("Multi Quality",deserializedJson.stream_data.file,true, extraNote = "StreamSb"),
                Video("Multi Quality",deserializedJson.stream_data.backup,true, extraNote = "StreamSbBackup")
            ),
            headers
        )
    }
}
