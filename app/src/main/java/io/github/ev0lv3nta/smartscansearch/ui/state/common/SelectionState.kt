package io.github.ev0lv3nta.smartscansearch.ui.state.common

data class SelectionState<T> (
    val selectedItems: Set<T> = emptySet(),
    val excludedItems: Set<T> = emptySet(),
    val selectAll: Boolean = false,
    val isSelecting: Boolean = false,
    val selectedCount: Int = 0,
)