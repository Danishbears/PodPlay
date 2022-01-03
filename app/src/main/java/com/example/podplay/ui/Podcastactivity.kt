package com.example.podplay

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.podplay.ui.PodcastDetailsFragment
import com.example.podplay.ui.adapter.PodcastListAdapter
import com.example.podplay.ui.db.PodPlayDatabase
import com.example.podplay.ui.repository.ItunesRepo
import com.example.podplay.ui.repository.PodcastRepo
import com.example.podplay.ui.service.FeedServicedada
import androidx.lifecycle.Observer
import androidx.work.*
import com.example.podplay.ui.EpisodePlayerFragment
import com.example.podplay.ui.model.Episode
import com.example.podplay.ui.service.ItunesService
import com.example.podplay.ui.service.RssFeedService
import com.example.podplay.ui.viewmodel.PodcastViewModel
import com.example.podplay.ui.viewmodel.SearchViewModel
import com.example.podplay.ui.worker.EpisodeUpdateWorker
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.*
import java.util.concurrent.TimeUnit


class Podcastactivity: AppCompatActivity(),PodcastListAdapter.PodcastListAdapterListener,PodcastDetailsFragment.OnPodcastDetailsListener {
    private val podcastViewModel by viewModels<PodcastViewModel>()
    private lateinit var searchMenuItem:MenuItem
    private  val TAG = javaClass.simpleName
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)


        val itunesService = ItunesService.instance
        val itunesRepo = ItunesRepo(itunesService)

        itunesRepo.searchByTerm("Android Developer"){
            Log.i(TAG,"Results = $it")
        }
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        addBackStackListener()
        scheduleJobs()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search,menu)

        if (menu != null) {
            searchMenuItem = menu.findItem(R.id.search_item)
        }

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })

        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if(supportFragmentManager.backStackEntryCount>0){ // to hide background recylce view after select
            podcastRecyclerView.visibility = View.INVISIBLE
        }

        if(podcastRecyclerView.visibility == View.INVISIBLE){
            searchMenuItem.isVisible = false
        }

        return true
    }

    private fun performSearch(term:String){

       /* val inutnesService = ItunesService.instance
        val itunesRepo = ItunesRepo(inutnesService)

        itunesRepo.searchByTerm(term){
            Log.i(TAG,"Result = $it")
        }*/

        showProgressBar()
        searchViewModel.seachPodcast(term){
            results ->
            hideProgressbar()
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun handleIntent(intent: Intent){
        if(Intent.ACTION_SEARCH == intent.action){
            val query = intent.getStringExtra(SearchManager.QUERY)?: return
            performSearch(query)
        }
        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if(podcastFeedUrl != null){
            podcastViewModel.setActivePodcast(podcastFeedUrl){
                it?.let{podcastSummaryView ->
                    onShowDetails(podcastSummaryView)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun setupToolbar(){
        setSupportActionBar(toolbar)
    }

    private fun setupViewModels(){
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        //podcastViewModel.podcastRepo = PodcastRepo()
        val rssService = FeedServicedada.instance
            // podcastViewModel.podcastRepo = PodcastRepo(rssService)
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(rssService,podcastDao)
    }

    private fun updateControls(){
        podcastRecyclerView.setHasFixedSize(true)
        val layoutmanager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutmanager

        val dividerItemDeco = DividerItemDecoration(
            podcastRecyclerView.context,layoutmanager.orientation
        )
        podcastRecyclerView.addItemDecoration(dividerItemDeco)

        podcastListAdapter = PodcastListAdapter(null,this,this)
        podcastRecyclerView.adapter = podcastListAdapter
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl?:return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData){
            hideProgressbar()
            if(it!=null){
                showDetailsFragment()
            }
            else{
                showError("Error loading feed $feedUrl")
            }
        }
    }

    private fun showProgressBar(){
        progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressbar(){
        progressBar.visibility = View.INVISIBLE
    }

    private  fun createdPodcastDetailsFragment():PodcastDetailsFragment{
        var podcastDetailsFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if(podcastDetailsFragment == null){
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun showDetailsFragment(){
        val podcastDetailsFragment = createdPodcastDetailsFragment()
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,podcastDetailsFragment, TAG_DETAILS_FRAGMENT
        ).addToBackStack("DetailsFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun showError(message:String){
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button),null)
            .create()
            .show()
    }

    private fun addBackStackListener(){
        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.backStackEntryCount == 0){
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onSubscribe() {
        podcastViewModel.saveActionPodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActionPodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData){
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    private fun showSubscribedPodcasts(){
        val podcasts = podcastViewModel.getPodcasts()?.value
        if(podcasts !=null){
            toolbar.title = getString(R.string.subscribe)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView(){
        podcastViewModel.getPodcasts()?.observe(this,Observer{
            if(it!=null){
                showSubscribedPodcasts()
            }
        })
    }

    private fun scheduleJobs(){
        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1,TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_EPISODE_UPDATE_JOB,
        ExistingPeriodicWorkPolicy.REPLACE,request)
    }

    private fun createEpisodePlayerFragment():EpisodePlayerFragment{
        var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as EpisodePlayerFragment?
        if(episodePlayerFragment == null){
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }
        return episodePlayerFragment
    }

    private fun showPlayerFragment(){
        val episodePlayerFragment = createEpisodePlayerFragment()
        supportFragmentManager.beginTransaction().replace(
            R.id.podcastDetailsContainer,episodePlayerFragment, TAG_PLAYER_FRAGMENT
        ).addToBackStack("PlayerFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    companion object{
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.example.podplay.episodes"
        private const val TAG_DETAILS_FRAGMENT ="DetailsFragment"
    }



}