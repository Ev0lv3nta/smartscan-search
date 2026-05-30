package com.fpf.smartscan.navigation

object Routes {
    const val SEARCH = "search"
    const val COLLECTIONS = "collections"

    const val COLLECTION_ITEMS = "collection_items"
    const val SETTINGS = "settings"
    const val SETTINGS_DETAIL = "settings_detail/{type}"
    const val DONATE = "donate"
    fun settingsDetail(type: String) = "settings_detail/$type"
}