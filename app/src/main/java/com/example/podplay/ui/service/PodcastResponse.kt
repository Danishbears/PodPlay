package com.example.podplay.ui.service

data class PodcastResponse(
    val resultCount:Int,
    val results:List<ItunesPodcast>
) {
    data class ItunesPodcast(
        val collectionCensoredName:String,
        val feedUrl:String,
        val artworkUrl100:String,
        val releaseData:String
    )
}