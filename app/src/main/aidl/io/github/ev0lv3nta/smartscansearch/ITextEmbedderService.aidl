package io.github.ev0lv3nta.smartscansearch;

import java.util.List;

interface ITextEmbedderService {
    int getEmbeddingDim();
    void closeSession();
    float[] embed(in String data);
    float[] embedBatch(in List<String> data);  // concatenate
    List<String> listModels();
    boolean selectModel(in String name);
}