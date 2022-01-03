package com.example.podplay.ui.service

import com.example.podplay.ui.viewmodel.PodcastViewModel
import java.util.*

data class RssFeedResponse( // Podcast
    var title:String = "",
    var description:String ="",
    var summary:String = "",
    var lastUpdated: Date = Date(),
    var episodes:MutableList<EpisodeResponse>? = null
){

data class EpisodeResponse( // Episode
    var title:String? = null,
    var link:String? = null,
    var description:String?= null,
    var guid:String? = null,
    var publicateDate:String? =null,
    var duration:String?= null,
    var Url:String? = null,
    var type:String? = null
)}