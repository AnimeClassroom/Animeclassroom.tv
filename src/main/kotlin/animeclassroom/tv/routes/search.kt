package animeclassroom.tv.routes

import animeclassroom.tv.dataclasses.*
import animeclassroom.tv.serviceMap
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.search() {
    post("/search/{service}/{provider}") {
        val mediaObj = call.receive<Media>()
        val service = call.parameters["service"]!!.lowercase()
        val provider = call.parameters["provider"]!!.lowercase()
        call.respond(
            serviceMap[service]!![provider]!!.search(mediaObj)
        )
    }
}