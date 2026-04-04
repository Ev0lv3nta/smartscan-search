package com.fpf.smartscan.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.fpf.smartscan.ITextEmbedderService
import com.fpf.smartscan.R
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.flattenEmbeddings
import com.fpf.smartscansdk.core.models.ModelManager
import com.fpf.smartscansdk.core.models.ModelName
import com.fpf.smartscansdk.core.models.ModelType
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder
import com.fpf.smartscansdk.ml.providers.embeddings.minilm.MiniLMTextEmbedder
import kotlinx.coroutines.runBlocking

class TextEmbedderAidlService: Service() {
    companion object {
        const val TAG = "TextEmbedderAidlService"
    }
    private lateinit var textEmbedder: TextEmbeddingProvider

    private var selectedModel = ModelName.CLIP_VIT_B_32_TEXT.name

    override fun onCreate() {
        super.onCreate()
        textEmbedder = ClipTextEmbedder(application, R.raw.clip_text_encoder_quant, vocabResId = R.raw.vocab, mergesResId = R.raw.merges)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        textEmbedder.closeSession()
    }

    private val binder = object : ITextEmbedderService.Stub() {

        override fun getEmbeddingDim(): Int {
            return textEmbedder.embeddingDim
        }

        override fun closeSession() {
            textEmbedder.closeSession()
        }

        override fun embed(data: String): FloatArray? {
            return runBlocking {
                try {
                    if (!textEmbedder.isInitialized()) textEmbedder.initialize()
                    val embedding = textEmbedder.embed(data)
                    embedding
                } catch (e: Exception) {
                    Log.d(TAG, "EMBEDDING_ERROR: ${e.message}")
                    null
                }
            }
        }

        override fun embedBatch(data: List<String>): FloatArray? {
            return runBlocking {
                try {
                    if(!textEmbedder.isInitialized()) textEmbedder.initialize()
                    val embeddings = textEmbedder.embedBatch(data)
                    val flattenedEmbeddings = flattenEmbeddings(embeddings, textEmbedder.embeddingDim)
                    flattenedEmbeddings
                }catch(e: Exception){
                    Log.d(TAG, "EMBEDDING_ERROR: ${e.message}")
                    null
                }
            }
        }

        override fun listModels(): List<String> {
            return ModelManager.listModels(application, ModelType.TEXT_ENCODER).map { it.name }
        }

        override fun selectModel(modelName: String): Boolean {
            if(modelName == selectedModel) return true

            val availableModels = listModels() + ModelName.CLIP_VIT_B_32_TEXT.name
            if(!availableModels.contains(modelName)) return false

            val model = ModelName.entries.firstOrNull { it.name == modelName }?: return false
            selectedModel = modelName

            textEmbedder = when(model){
                ModelName.ALL_MINILM_L6_V2 -> {
                    textEmbedder.closeSession()
                    MiniLMTextEmbedder(application)
                }
                ModelName.CLIP_VIT_B_32_TEXT -> {
                    textEmbedder.closeSession()
                    ClipTextEmbedder(application, R.raw.clip_text_encoder_quant, vocabResId = R.raw.vocab, mergesResId = R.raw.merges)
                }
                else -> return false
            }
            return true
        }
    }
}