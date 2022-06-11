package animeclassroom.tv.routes

import animeclassroom.tv.dataclasses.PostURL
import animeclassroom.tv.parser.*
import animeclassroom.tv.serviceMap
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.loadVideoServer() {
    post("/loadvideoserver/{service}/{provider}") {
        val mediaObj = call.receive<PostURL>().also { println(it) }
        val arrayOfVideoServer = mutableListOf<VideoServer>()
        val service = call.parameters["service"]!!.lowercase()
        val provider = call.parameters["provider"]!!.lowercase()
        serviceMap[service]!![provider]!!.loadVideoServers(mediaObj.url){
            arrayOfVideoServer.add(it)
        }
       call.respond(arrayOfVideoServer)
    }
}