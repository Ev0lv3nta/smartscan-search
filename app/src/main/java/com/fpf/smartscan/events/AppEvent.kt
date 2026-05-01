package com.fpf.smartscan.events

enum class AppEventType {
    RESTORE_SUCCESS,
    RESTORE_FAILED,
    BACKUP_SUCCESS,
    BACKUP_FAILED,
    MODEL_IMPORT_SUCCESS,
    MODEL_IMPORT_FAILED
}
data class AppEvent (
    val type: AppEventType,
    val message: String
)