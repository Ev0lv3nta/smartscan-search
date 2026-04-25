package com.fpf.smartscan.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File


fun getImageUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

fun openImageInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}


fun getVideoUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

fun openVideoInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}


fun queryImageIds(
    context: Context,
    dirUris: List<Uri> = emptyList(),
    startDate: Long? = null,
    endDate: Long? = null
): List<Long> {
    val imageIds = mutableListOf<Long>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()
    val envRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

    if (dirUris.isNotEmpty()) {
        for (uri in dirUris) {
            try {
                if (DocumentsContract.isTreeUri(uri) || uri.authority == "com.android.externalstorage.documents") {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val afterColon = docId.substringAfter(':', "")
                    if (afterColon.isNotEmpty()) {
                        val rel = afterColon.trim('/') + "/"
                        selectionParts.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                        continue
                    }
                }

                if (uri.scheme == "file") {
                    val file = File(uri.path ?: continue)
                    val absPath = file.absolutePath.trimEnd('/') + "/"
                    if (absPath.startsWith(envRoot)) {
                        val rel = absPath.removePrefix(envRoot).trimStart('/').trimEnd('/') + "/"
                        selectionParts.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                    }
                    continue
                }

                val seg = uri.path?.trim('/') ?: uri.lastPathSegment ?: continue
                if (seg.isNotEmpty()) {
                    val folderLike = if (seg.endsWith("/")) seg else "$seg/"
                    selectionParts.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                    selectionArgs.add("%$folderLike%")
                }
            } catch (_: Exception) {
            }
        }
    }

    // Date filters (DATE_ADDED is in seconds)
    if (startDate != null) {
        selectionParts.add("${MediaStore.Images.Media.DATE_ADDED} >= ?")
        selectionArgs.add(startDate.toString())
    }
    if (endDate != null) {
        selectionParts.add("${MediaStore.Images.Media.DATE_ADDED} <= ?")
        selectionArgs.add(endDate.toString())
    }

    val selection: String?
    val args: Array<String>?
    if (selectionParts.isEmpty()) {
        selection = null
        args = null
    } else {
        selection = selectionParts.joinToString(" AND ", prefix = "(", postfix = ")")
        args = selectionArgs.toTypedArray()
    }

    context.applicationContext.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        args,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            imageIds.add(cursor.getLong(idColumn))
        }
    }
    return imageIds
}

fun queryVideoIds(
    context: Context,
    dirUris: List<Uri> = emptyList(),
    startDate: Long? = null,
    endDate: Long? = null
): List<Long> {
    val videoIds = mutableListOf<Long>()
    val projection = arrayOf(MediaStore.Video.Media._ID)
    val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()
    val envRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

    if (dirUris.isNotEmpty()) {
        for (uri in dirUris) {
            try {
                if (DocumentsContract.isTreeUri(uri) || uri.authority == "com.android.externalstorage.documents") {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val afterColon = docId.substringAfter(':', "")
                    if (afterColon.isNotEmpty()) {
                        val rel = afterColon.trim('/') + "/"
                        selectionParts.add("${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                        continue
                    }
                }

                if (uri.scheme == "file") {
                    val file = File(uri.path ?: continue)
                    val absPath = file.absolutePath.trimEnd('/') + "/"
                    if (absPath.startsWith(envRoot)) {
                        val rel = absPath.removePrefix(envRoot).trimStart('/').trimEnd('/') + "/"
                        selectionParts.add("${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                    }
                    continue
                }

                val seg = uri.path?.trim('/') ?: uri.lastPathSegment ?: continue
                if (seg.isNotEmpty()) {
                    val folderLike = if (seg.endsWith("/")) seg else "$seg/"
                    selectionParts.add("${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                    selectionArgs.add("%$folderLike%")
                }
            } catch (_: Exception) {
            }
        }
    }

    if (startDate != null) {
        selectionParts.add("${MediaStore.Video.Media.DATE_ADDED} >= ?")
        selectionArgs.add(startDate.toString())
    }

    if (endDate != null) {
        selectionParts.add("${MediaStore.Video.Media.DATE_ADDED} <= ?")
        selectionArgs.add(endDate.toString())
    }

    val selection: String?
    val args: Array<String>?
    if (selectionParts.isEmpty()) {
        selection = null
        args = null
    } else {
        selection = selectionParts.joinToString(" AND ", prefix = "(", postfix = ")")
        args = selectionArgs.toTypedArray()
    }

    context.applicationContext.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        args,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        while (cursor.moveToNext()) {
            videoIds.add(cursor.getLong(idColumn))
        }
    }

    return videoIds
}

fun shareMedia(context: Context, uri: Uri){
    val mime = context.contentResolver.getType(uri)
    val shareIntent: Intent = Intent().apply {
        this.action = Intent.ACTION_SEND
        this.putExtra(Intent.EXTRA_STREAM, uri)
        this.type = mime
    }
    if(!mime.isNullOrBlank()){
        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}

fun shareMediaMulti(context: Context, uris: List<Uri>){
    val mime = context.contentResolver.getType(uris[0])?.substringBefore("/")?.plus( "/*")
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = mime
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    if(!mime.isNullOrBlank()){
        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}

fun filterAccessibleMediaStoreIds(
    context: Context,
    ids: List<Long>,
    mediaType: MediaType
): Pair<List<Long>, List<Long>> {

    if (ids.isEmpty()) return emptyList<Long>() to emptyList()

    val uri = when (mediaType) {
        MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val selection = "${MediaStore.MediaColumns._ID} IN (${
        ids.joinToString(",") { "?" }
    })"

    val selectionArgs = ids.map { it.toString() }.toTypedArray()

    val existingIds = HashSet<Long>()

    context.contentResolver.query(
        uri,
        arrayOf(MediaStore.MediaColumns._ID),
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        while (cursor.moveToNext()) {
            existingIds.add(cursor.getLong(idIndex))
        }
    }

    val (valid, invalid) = ids.partition { it in existingIds }
    return valid to invalid
}
