package com.moblie.ketchupapp.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.moblie.ketchupapp.R
import com.moblie.ketchupapp.model.VideoModel
import com.moblie.ketchupapp.ui.VideoActivity
import com.moblie.ketchupapp.ui.VideoActivity.Companion.EXTRA_ID
import com.moblie.ketchupapp.ui.VideoActivity.Companion.EXTRA_TITLE
import com.moblie.ketchupapp.ui.adapter.LoadStateAdapter
import com.moblie.ketchupapp.ui.adapter.VideoListPageAdapter
import com.moblie.ketchupapp.ui.adapter.diffutils.VideoDiffUtils
import com.moblie.ketchupapp.utils.asMergedLoadStates
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter

abstract class BaseVideoListFragment : BaseViewStubFragment() {

    abstract val listData: LiveData<PagingData<VideoModel>>

    abstract val header: RecyclerView.Adapter<*>?

    private val pagingAdapter = VideoListPageAdapter(VideoDiffUtils) {
        val intent = Intent(this.context, VideoActivity::class.java).apply {
            putExtra(EXTRA_ID, it.id)
            putExtra(EXTRA_TITLE, it.title)
        }
        startActivity(intent)
    }

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreateViewAfterViewStubInflated(inflatedView: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout = inflatedView as SwipeRefreshLayout
        recyclerView = swipeRefreshLayout.findViewById(R.id.recycler_view)
        setupRecyclerView()

        swipeRefreshLayout.setOnRefreshListener {
            pagingAdapter.refresh()
        }
    }

    override fun getViewStubLayoutResource(): Int {
        return R.layout.fragment_video_list
    }

    private fun setupRecyclerView() {
        val adapter = pagingAdapter.withLoadStateFooter(LoadStateAdapter { pagingAdapter.retry() })
        recyclerView.adapter = if (header == null) adapter else ConcatAdapter(header, adapter)
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        lifecycleScope.launchWhenCreated {
            pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                swipeRefreshLayout.isRefreshing = loadStates.mediator?.refresh is LoadState.Loading
            }
            pagingAdapter.loadStateFlow.asMergedLoadStates()
                // Only emit when REFRESH changes, as we only want to react on loads replacing the
                // list.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                // Scroll to top is synchronous with UI updates, even if remote load was triggered.
                .collect {
                    recyclerView.scrollToPosition(0)
                }
        }
        listData.observe(viewLifecycleOwner) { data ->
            data.let {
                lifecycleScope.launchWhenCreated {
                    pagingAdapter.submitData(it)
                }
            }
        }
    }
}