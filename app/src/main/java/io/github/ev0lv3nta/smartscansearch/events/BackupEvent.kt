package io.github.ev0lv3nta.smartscansearch.events

enum class BackupEventType {
    RESTORE,
    BACKUP,
}
data class BackupEvent (
    val type: BackupEventType,
    val success: Boolean,
    val message: String? = null
)