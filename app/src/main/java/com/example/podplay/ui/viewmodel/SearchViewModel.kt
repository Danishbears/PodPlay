package com.example.podplay.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.podplay.ui.repository.ItunesRepo
import com.example.podplay.ui.service.PodcastResponse
import com.example.podplay.ui.util.DateUtils

class SearchViewModel(application:Application):AndroidViewModel(application) {
    var iTunesRepo: ItunesRepo? = null

    data class PodcastSummaryViewData(
        var name:String? = "",
        var lastUpdated:String? ="",
        var imageUrl:String = "",
        var feedUrl:String?=""
    )

    private fun itunesPodcastToPodcastSummaryView(
        itunesPodcast: PodcastResponse.ItunesPodcast
    ):PodcastSummaryViewData{
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseData),
            itunesPodcast.artworkUrl100,
            itunesPodcast.feedUrl
        )
    }

    fun seachPodcast(term:String,callback:(List<PodcastSummaryViewData>) ->Unit){
        iTunesRepo?.searchByTerm(term){
                results ->
            if(results == null){
                callback(emptyList())
            }
            else{
                val searchViews = results.map{
                        podcast -> itunesPodcastToPodcastSummaryView(podcast)
                }
                callback(searchViews)
            }
        }
    }
}