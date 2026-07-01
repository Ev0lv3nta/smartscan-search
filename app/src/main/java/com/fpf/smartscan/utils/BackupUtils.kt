package com.fpf.smartscan.utils

import android.content.Context
import android.net.Uri
import com.fpf.smartscan.constants.EmbeddingStoresFilesQuant
import com.fpf.smartscan.data.MediaDatabase
import java.io.File

object BackupUtils {
    const val BACKUP_FILENAME = "smartscan_backup.zip"
    private const val HASH_FILENAME = "hash.txt"
    
    suspend fun backup(context: Context, outputUri: Uri){
        val indexZipFile = File(context.cacheDir, BACKUP_FILENAME)
        val imageEmbeddingStoreFile = File(context.filesDir, EmbeddingStoresFilesQuant.IMAGE)
        val videoEmbeddingStoreFile = File(context.filesDir,  EmbeddingStoresFilesQuant.VIDEO)
        val clusterEmbeddingStoreFile = File(context.filesDir, EmbeddingStoresFilesQuant.CLUSTER)
        val hashFile = File(context.cacheDir, HASH_FILENAME)
        val dbPath = context.getDatabasePath(MediaDatabase.DB_NAME)

        val embedStoreFiles = listOf(imageEmbeddingStoreFile, videoEmbeddingStoreFile, clusterEmbeddingStoreFile)
        val filesToZip = listOf( hashFile, dbPath) + embedStoreFiles

        try {
            if(embedStoreFiles.none{it.exists()}) error("Missing index file(s)")
            val hashes: List<String> = filesToZip.filter { it.exists() && it != hashFile }.map{hashFile(it)}
            hashFile.writeText(hashes.joinToString("\n") )
            zipFiles(indexZipFile, filesToZip)
            copyToUri(context, outputUri, indexZipFile)
        }finally {
            indexZipFile.delete()
            hashFile.delete()
        }

    }

    suspend fun restore(context: Context, uri: Uri){
        val indexZipFile = File(context.cacheDir, BACKUP_FILENAME)
        MediaDatabase.close()
        try {
            copyFromUri(context, uri, indexZipFile)
            val extractedFiles = unzipFiles(indexZipFile, context.filesDir)

            if(!isValidBackupFile(extractedFiles)){
                extractedFiles.forEach { it.delete() }
                error("Invalid backup file")
            }
        }finally {
            indexZipFile.delete()
        }

    }

    private suspend fun isValidBackupFile(extractedFiles: List<File>): Boolean{
        val hashFile = extractedFiles.find { it.name == HASH_FILENAME }?: return false
        val hashesFromFile: List<String> = hashFile.readLines()
        if(hashesFromFile.isEmpty()) return false

        val otherFiles = extractedFiles.filterNot{it.name == HASH_FILENAME}
        val otherFileHashes = otherFiles.map{hashFile(it)}
        return hashesFromFile.toSet() == otherFileHashes.toSet()
    }
}