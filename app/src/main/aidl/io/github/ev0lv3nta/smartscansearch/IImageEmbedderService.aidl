// IImageEmbedderService.aidl
package io.github.ev0lv3nta.smartscansearch;

import java.util.List;

interface IImageEmbedderService {
    int getEmbeddingDim();
    byte[] getDelimiter();
    void closeSession();
    float[] embed(in byte[] data);
    float[] embedBatch(in byte[] data); //inputs and outputs are concatenated
    List<String> listModels();
    boolean selectModel(in String name);
}