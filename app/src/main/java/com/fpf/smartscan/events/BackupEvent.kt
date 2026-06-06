package com.fpf.smartscan.events

enum class BackupEventType {
    RESTORE,
    BACKUP,
}
data class BackupEvent (
    val type: BackupEventType,
    val success: Boolean,
    val message: String? = null
)