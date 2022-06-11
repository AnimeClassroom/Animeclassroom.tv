package animeclassroom.tv.extractors

import animeclassroom.tv.parser.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class GogoCDN : Extractor() {
    private data class JsonAjaxResponse(
        val data: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class DecryptedJsonResponse(
        val source:List<GogoSource>,
        val source_bk:List<GogoSource>,
    ){
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class GogoSource(val file:String, val label:String, val type:String)
    }

    private fun cryptoHandler(string:String,encrypt:Boolean=true) : String {
        val key = "63976882873559819639988080820907".toByteArray()
        val secretKey =  SecretKeySpec(key, "AES")

        val iv = "4770478969418267".toByteArray()
        val ivParameterSpec =  IvParameterSpec(iv)

        val padding = byteArrayOf(0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8)

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        return if (!encrypt) {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            String(cipher.doFinal(Base64.getDecoder().decode(string)))
        }
        else{
            cipher.init(Cipher.ENCRYPT_MODE,secretKey,ivParameterSpec)
            Base64.getEncoder().encodeToString(cipher.doFinal(string.toByteArray()+padding))
        }
    }

    override fun getStreamLinks(name:String, url: String): VideoServer{
        val response = HttpClient.newCall(Request.Builder().url(url).build()).execute().body!!.string()
        val returnVideoList = mutableListOf<Video>()
        if(url.contains("streaming.php")) {
            Jsoup.parse(response).select("script[data-name='episode']").attr("data-value").also { token ->
                val id = cryptoHandler(cryptoHandler(token, false).substringBefore('&'), true)
                val dataFromJson = jacksonObjectMapper().readValue<JsonAjaxResponse>(
                    HttpClient.newCall(
                        Request.Builder().url("https://${URI(url).host}/encrypt-ajax.php?id=$id")
                            .header("X-Requested-With", "XMLHttpRequest")
                            .build()
                    ).execute().body!!.string()
                )
                val gogoObject = jacksonObjectMapper().readValue<DecryptedJsonResponse>(
                    cryptoHandler(dataFromJson.data,false).replace("o\"<P{#mem","e\":[{\"fil")
                )

                (gogoObject.source + gogoObject.source_bk).forEach{
                    returnVideoList.add(Video(
                        name = if(it.type == "hls") "Multi Quality" else it.label,
                        url = it.file,
                        isM3U8 = (it.type == "hls")
                    ))
                }
            }
        }

        return VideoServer(name, returnVideoList, mutableMapOf("referer" to url))
    }
}