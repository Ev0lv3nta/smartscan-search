package com.fpf.smartscan.di

import org.koin.core.module.dsl.viewModel
import com.fpf.smartscan.MainViewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        MainViewModel(
            application = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE)
        )
    }
}