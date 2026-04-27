package com.fpf.smartscan.di

import org.koin.core.module.dsl.viewModel
import com.fpf.smartscan.MainViewModel
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        MainViewModel(
            application = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE)
        )
    }
    viewModel {
        SearchViewModel(
            application = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            imageClusterStore = get(IMAGE_CLUSTER_STORE),
            videoClusterStore = get(VIDEO_CLUSTER_STORE)
        )
    }
}