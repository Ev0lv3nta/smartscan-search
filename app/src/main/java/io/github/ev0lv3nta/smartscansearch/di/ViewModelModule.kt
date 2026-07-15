package io.github.ev0lv3nta.smartscansearch.di

import org.koin.core.module.dsl.viewModel
import io.github.ev0lv3nta.smartscansearch.MainViewModel
import io.github.ev0lv3nta.smartscansearch.ui.screens.collections.CollectionItemsViewModel
import io.github.ev0lv3nta.smartscansearch.ui.screens.collections.CollectionsViewModel
import io.github.ev0lv3nta.smartscansearch.ui.screens.search.SearchViewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        MainViewModel(
            application = get(),
            db = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            clusterStore = get(CLUSTER_STORE),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get()
            )
    }
    viewModel {
        SearchViewModel(
            application = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            clusterStore = get(CLUSTER_STORE),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get()
        )
    }
    viewModel {
        CollectionItemsViewModel(
            application = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get(),
            clusterStore = get(CLUSTER_STORE),
            )
    }

    viewModel {
        CollectionsViewModel(
            application = get(),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            clusterStore = get(CLUSTER_STORE),
            )
    }
}