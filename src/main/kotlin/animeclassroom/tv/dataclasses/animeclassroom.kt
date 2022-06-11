package animeclassroom.tv.dataclasses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Media(
    val id: Int?,
    val isAdult: Boolean?,
    val status: String?,
    val chapters: Int?,
    val episodes: Int?,
    val nextAiringEpisode: AnimeClassroomEpisode?,
    val title : AnimeClassroomTitle?,
    val startDate: AnimeClassroomDate?,
    val endDate: AnimeClassroomDate?,
    val genres:List<String>?,
    val seasonYear:Int?,
    val idMal: Int?,
    val countryOfOrigin: String?,
    val format: String?,
    val season:String?,
){
    data class AnimeClassroomEpisode(val airingAt: Int?, val episode: Int?)
    data class AnimeClassroomTitle(val english: String?, val romaji: String?, val userPreferred: String?)
    data class AnimeClassroomDate(val day:Int?, val month:Int?, val year: Int?)
}