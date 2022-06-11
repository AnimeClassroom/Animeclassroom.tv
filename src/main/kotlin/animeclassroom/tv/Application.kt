package animeclassroom.tv

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import animeclassroom.tv.parser.Parser
import animeclassroom.tv.parser.sources.*
import animeclassroom.tv.plugins.*
import io.ktor.server.plugins.cors.routing.*

val serviceMap = mutableMapOf<String,Map<String, Parser>>(
    "anime" to mapOf(
        "nineanime" to NineAnime(),
        "gogoanime" to GogoAnime(),
        "twist" to Twist(),
        "tenshi" to Tenshi()
    ),
//    "manga" to mapOf(
//        "mangabuddy" to Nothing(),
//        "mangasee" to Nothing(),
//        "mangapill" to Nothing(),
//        "mangadex" to Nothing(),
//        "mangareader" to Nothing()
//    )
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CORS){
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.AccessControlAllowHeaders)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            anyHost()
        }
        install(ContentNegotiation){
            jackson{
                configure(SerializationFeature.INDENT_OUTPUT, true)
                setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                })
            }
        }
        configureRouting()
    }.start(wait = true)
}