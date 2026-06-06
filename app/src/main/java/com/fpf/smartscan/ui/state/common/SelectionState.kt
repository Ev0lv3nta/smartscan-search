package com.fpf.smartscan.ui.state.common

data class SelectionState<T> (
    val selectedItems: Set<T> = emptySet(),
    val excludedItems: Set<T> = emptySet(),
    val selectAll: Boolean = false,
    val isSelecting: Boolean = false,
    val selectedCount: Int = 0,
)