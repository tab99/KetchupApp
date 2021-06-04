package com.moblie.ketchupapp.repository

import androidx.lifecycle.LiveData
import androidx.paging.*
import com.moblie.ketchupapp.api.HQVideoService
import com.moblie.ketchupapp.model.VideoModel
import com.moblie.ketchupapp.paging.HomeRemoteMediator
import com.moblie.ketchupapp.paging.SearchRemoteMediator
import com.moblie.ketchupapp.room.KetchupDatabase
import com.moblie.ketchupapp.utils.Environment
import com.moblie.ketchupapp.utils.PageTag
import javax.inject.Inject

class SearchRepositoryImpl  @Inject constructor(
    val database: KetchupDatabase,
    private val backend: HQVideoService
) : SearchRepository {


    @ExperimentalPagingApi
    override fun getVideos(query: String): LiveData<PagingData<VideoModel>> {
        val mediator = SearchRemoteMediator(query, database, backend)
        return Pager(
            // Configure how data is loaded by passing additional properties to
            // PagingConfig, such as prefetchDistance.
            PagingConfig(pageSize = Environment.NETWORK_PAGE_SIZE),
            remoteMediator = mediator
        ) {
            database.videoDao().pagingSource(PageTag.SEARCH(query))
        }.liveData
    }
}