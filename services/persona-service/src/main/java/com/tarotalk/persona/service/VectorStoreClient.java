package com.tarotalk.persona.service;

import java.util.List;

public interface VectorStoreClient {
    void upsert(String id, double[] vector, String payload);
    List<String> query(double[] vector, int topK);
}
