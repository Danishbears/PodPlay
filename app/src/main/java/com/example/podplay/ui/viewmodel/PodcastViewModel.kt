package com.example.podplay.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.podplay.ui.model.Episode
import com.example.podplay.ui.model.Podcast
import com.example.podplay.ui.repository.PodcastRepo
import com.example.podplay.ui.util.DateUtils
import java.util.*

class PodcastViewModel(application: Application):AndroidViewModel(application) {
    var livePodcastData:LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null
    private var activePodcast:Podcast? = null
    var activeEpisodeViewData:EpisodeViewData? = null
    var podcastRepo:PodcastRepo? = null
    var activePodcastViewData:PodcastViewData? = null

    data class PodcastViewData(
        var subscribed:Boolean = false,
        var feedTitle:String? = "",
        var feedUrl:String? = "",
        var feedDesc:String?="",
        var imageUrl:String?="",
        var episodes:List<EpisodeViewData>
    )

    data class EpisodeViewData(
        var guid:String?="",
        var title:String? = "",
        var description:String?="",
        var mediaUrl:String?="",
        var releaseDate: Date? =null,
        var duration:String?="",
        var isVideo:Boolean = false
    )

    private fun episodesToEpisodesView(episodes:List<Episode>):List<EpisodeViewData>{
        return episodes.map{
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(it.guid,it.title,it.description,it.mediaUrl,it.releaseDate,it.duration,isVideo)
        }
    }

    private fun podcastToPodcastView(podcast: Podcast):PodcastViewData{
        return PodcastViewData(
            podcast.id !=null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }

    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData,callback:(PodcastViewData?)->Unit){
        val repo = podcastRepo ?:return
        val feedUrl = podcastSummaryViewData.feedUrl?:return

        repo.getPodcast(feedUrl){
            it?.let {
                it.feedTitle = podcastSummaryViewData.name ?:""
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it

                callback(activePodcastViewData)
            }
        }
    }

    fun saveActionPodcast(){
        val repo=podcastRepo?:return
        activePodcast?.let{
            it.episodes = it.episodes.drop(1)
            repo.save(it)
        }
    }

    private fun podcastToSummaryView(podcast:Podcast):SearchViewModel.PodcastSummaryViewData{
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dataToShortDate(podcast.lasUpdate),
            podcast.imageUrl,
            podcast.feedUrl
        )
    }

    fun getPodcasts():LiveData<List<SearchViewModel.PodcastSummaryViewData>>?{
        val repo  = podcastRepo ?:return null
        if(livePodcastData == null){
            val liveData = repo.getAll()
            livePodcastData = Transformations.map(liveData){
                podcastList ->
                podcastList.map{podcast->
                    podcastToSummaryView(podcast)
                }
            }
        }
        return livePodcastData
    }

    fun deleteActionPodcast(){
        val repo = podcastRepo ?:return
        activePodcast?.let{
            repo.delete(it)
        }
    }

    fun setActivePodcast(feedUrl:String,callback:(SearchViewModel.PodcastSummaryViewData?)->Unit){
    val repo = podcastRepo ?: return
    repo.getPodcast(feedUrl){
        if(it ==null){
            callback(null)
        }else{
            activePodcastViewData = podcastToPodcastView(it)
            activePodcast = it
            callback(podcastToSummaryView(it))
        }
    }
    }


}