package animeclassroom.tv.plugins

import animeclassroom.tv.routes.*
import io.ktor.server.routing.*
import io.ktor.server.application.*

fun Application.configureRouting() {
    routing {
        search()
        loadEpisodes()
        loadVideoServer()
        malInfo()
    }
}