package com.example.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podplay.R
import com.example.podplay.ui.adapter.EpisodeListAdapter
import com.example.podplay.ui.service.PodplayMediaService
import com.example.podplay.ui.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import java.lang.RuntimeException

class PodcastDetailsFragment: Fragment(),EpisodeListAdapter.EpisodeListAdapterListener {
    private lateinit var mediaBrowser:MediaBrowserCompat
    //private var mediaControllerCallback:MediaControllerCallback?=null
    private var menuItem:MenuItem? = null
    private var listener:OnPodcastDetailsListener?=null
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private val podcastViewModel:PodcastViewModel by activityViewModels()

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        //initMediaBrowser()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details,container,false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details,menu)
        menuItem = menu.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    private fun updateControls(){
        val viewData = podcastViewModel.activePodcastViewData ?: return

        feedTitleTExtView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let{
           activity ->
           Glide.with(activity)
               .load(viewData.imageUrl)
               .into(feedImageView)
        }
    }

    private fun setupControls(){
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            episodeRecyclerView.context,layoutManager.orientation
        )
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)
        episodeListAdapter = EpisodeListAdapter(
            podcastViewModel.activePodcastViewData?.episodes
        ,this)
        episodeRecyclerView.adapter = episodeListAdapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is OnPodcastDetailsListener){
            listener = context
        }else{
            throw RuntimeException(context.toString()+"must implement onPodcastDetailsListener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_feed_action ->{
                podcastViewModel.activePodcastViewData?.feedUrl?.let{
                   if(podcastViewModel.activePodcastViewData?.subscribed == true ){
                       listener?.onUnsubscribe()
                   }
                    else{
                        listener?.onSubscribe()
                   }
                }
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

    }

    private fun updateMenuItem(){
        val viewData = podcastViewModel.activePodcastViewData ?:return
        menuItem?.title = if(viewData.subscribed)
            getString(R.string.unsubscribe) else
                getString(R.string.subscribe)
    }

    /*private fun registerMediaController(token:MediaSessionCompat.Token){
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity,token)
        MediaControllerCompat.setMediaController(fragmentActivity,mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }*/

    /*private fun initMediaBrowser(){
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity, ComponentName(fragmentActivity,PodplayMediaService::class.java),
            MediaBrowserCallBacks(),null)
    }*/

    /*private fun startPlaying(episodeViewData:PodcastViewModel.EpisodeViewData){
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
       // controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl),null)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
            viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl)
        controller.transportControls.playFromUri(
            Uri.parse(episodeViewData.mediaUrl), bundle)

    }*/

    override fun onStart() {
        super.onStart()
        /*if(mediaBrowser.isConnected){
            val fragmentActivity = activity as FragmentActivity
            if(MediaControllerCompat.getMediaController(fragmentActivity) == null){
                registerMediaController(mediaBrowser.sessionToken)
            }
        }else{
            mediaBrowser.connect()
        }*/
    }

    override fun onStop() {
        super.onStop()
       /* val fragmentActivity = activity as FragmentActivity
        if(MediaControllerCompat.getMediaController(fragmentActivity) == null){
            mediaControllerCallback?.let{
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }*/
    }

    companion object{
        fun newInstance():PodcastDetailsFragment{
            return PodcastDetailsFragment()
        }
    }

  /* inner class MediaControllerCallback:MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")

        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("Static changed to $state")
        }}

        inner class MediaBrowserCallBacks:MediaBrowserCompat.ConnectionCallback(){

            override fun onConnected() {
                super.onConnected()
                registerMediaController(mediaBrowser.sessionToken)
                println("onConnected")
            }

            override fun onConnectionSuspended() {
                super.onConnectionSuspended()
                println("onConnectionSuspended")
                //Disable transport controls
            }

            override fun onConnectionFailed() {
                super.onConnectionFailed()
                println("onConnectionFailed")
            }
        }*/

    interface OnPodcastDetailsListener{
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData:PodcastViewModel.EpisodeViewData)
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        /*val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if(controller.playbackState != null){
            if(controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                controller.transportControls.pause()
            }else{
                startPlaying(episodeViewData)
            }
        }else{
            startPlaying(episodeViewData)
        }*/
        listener?.onShowEpisodePlayer(episodeViewData)
    }
}