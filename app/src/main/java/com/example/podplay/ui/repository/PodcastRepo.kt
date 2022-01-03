package com.example.podplay.ui.repository

import androidx.lifecycle.LiveData
import com.example.podplay.ui.db.PodcastDao
import com.example.podplay.ui.model.Episode
import com.example.podplay.ui.model.Podcast
import com.example.podplay.ui.service.FeedServicedada
import com.example.podplay.ui.service.RssFeedResponse
import com.example.podplay.ui.service.RssFeedService
import com.example.podplay.ui.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService:FeedServicedada,private var podcastDao: PodcastDao)
{

    fun getPodcast(feedUrl:String,callback:(Podcast?)->Unit){
       /* val rssFeedService = RssFeedService()  // old version of getPodcast!
        rssFeedService.getFeed(feedUrl){}
        callback(
            Podcast(feedUrl,"No Name","No description","No image")
        )*/

        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)
            if(podcast != null){
                podcast.id?.let{
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            }else{

            }
        }

    feedService.getFeed(feedUrl){feedResponse->
        var podcast:Podcast? = null
        if(feedResponse !=null){
            podcast = rssResponsePodcast(feedUrl,"",feedResponse)
        }
        GlobalScope.launch(Dispatchers.Main) {
            callback(podcast)
        }
    }

    }

    private fun rssItemsEpisodes(episodeResponse:List<RssFeedResponse.EpisodeResponse>):List<Episode>{
        return episodeResponse.map{
            Episode(
                it.guid ?:"",
                null,
                it.title?:"",
                it.description?:"",
                it.Url?:"",
                it.type?:"",
                DateUtils.xmlDateToDate(it.publicateDate),
                it.duration ?:""
            )
        }
    }
    private fun rssResponsePodcast(feedUrl:String,imageUrl:String,rssResponse:RssFeedResponse):Podcast?{
        val items = rssResponse.episodes?:return null
        val description = if(rssResponse.description =="")
            rssResponse.summary else rssResponse.description
        return Podcast(null,feedUrl,rssResponse.title,description,imageUrl,rssResponse.lastUpdated,episodes = rssItemsEpisodes(items))
    }

    fun save(podcast:Podcast){
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for(episode in podcast.episodes){
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast:Podcast){
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    fun getAll():LiveData<List<Podcast>>{
        return podcastDao.loadPodcasts()
    }

    private fun getNewEpisodes(localPodcast:Podcast,callback: (List<Episode>) -> Unit){
        feedService.getFeed(localPodcast.feedUrl){response ->
            if(response != null){
                val remotePodcast = rssResponsePodcast(localPodcast.feedUrl,localPodcast.imageUrl,response)
                    remotePodcast?.let{
                        val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)

                        val newEpisodes = remotePodcast.episodes.filter { episode ->
                            localEpisodes.find{
                                episode.guid == it.guid
                            } ==null
                        }
                        callback(newEpisodes)
                    }
            }else{
                callback(listOf())
            }
        }
    }

    private fun saveMewEpisodes(podcastId:Long,episodes:List<Episode>){
        GlobalScope.launch {
            for(episode in episodes){
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun updatePodcastEpisodes(callback:(List<PodcastUpdateInfo>) -> Unit){
        val updatePodcasts:MutableList<PodcastUpdateInfo> = mutableListOf()
        val podcasts = podcastDao.loadPodcastStatic()
        var processCount = podcasts.count()
        for(podcast in podcasts){
            getNewEpisodes(podcast){
                newEpisodes ->
                if(newEpisodes.count()>0){
                    saveMewEpisodes(podcast.id!!,newEpisodes)
                    updatePodcasts.add(PodcastUpdateInfo(podcast.feedUrl,podcast.feedTitle
                    ,newEpisodes.count()))
                }
                processCount--
                if(processCount == 0){
                    callback(updatePodcasts)
                }
            }
        }
    }

    class PodcastUpdateInfo(val feedUrl:String,val name:String,val newCount:Int)
}

