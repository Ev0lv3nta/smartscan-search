package io.github.ev0lv3nta.smartscansearch.di

import io.github.ev0lv3nta.smartscansearch.data.MediaDatabase
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterCrossRefRepository
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterMetadataRepository
import io.github.ev0lv3nta.smartscansearch.data.metadata.MediaMetadataRepository
import io.github.ev0lv3nta.smartscansearch.data.tags.TagCrossRefRepository
import io.github.ev0lv3nta.smartscansearch.data.tags.TagRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dbModule = module {

    single {
        MediaDatabase.getDatabase(androidApplication())
    }

    single { get<MediaDatabase>().metadataDao() }
    single { get<MediaDatabase>().tagDao() }
    single { get<MediaDatabase>().tagCrossRefDao() }
    single { get<MediaDatabase>().clusterCrossRefDao() }
    single { get<MediaDatabase>().clusterMetadataDao() }

    single { MediaMetadataRepository(get()) }
    single { TagRepository(get()) }
    single { TagCrossRefRepository(get()) }
    single { ClusterCrossRefRepository(get()) }
    single { ClusterMetadataRepository(get()) }
}