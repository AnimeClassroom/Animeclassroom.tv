package animeclassroom.tv.routes

import animeclassroom.tv.dataclasses.PostURL
import animeclassroom.tv.serviceMap
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.loadEpisodes() {
    post("/loadepisodes/{service}/{provider}") {
        val mediaObj = call.receive<PostURL>().also { println(it) }
        val service = call.parameters["service"]!!.lowercase()
        val provider = call.parameters["provider"]!!.lowercase()
        call.respond(
            serviceMap[service]!![provider]!!.loadEpisodes(mediaObj.url)
        )
    }
}