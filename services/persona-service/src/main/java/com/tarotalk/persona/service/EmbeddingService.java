package com.tarotalk.persona.service;

import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    public double[] embed(String text) {
        double[] vector = new double[8];
        if (text == null) {
            return vector;
        }
        int hash = text.hashCode();
        for (int i = 0; i < vector.length; i++) {
            vector[i] = ((hash >> i) & 0xFF) / 255.0;
        }
        return vector;
    }

    public String serialize(double[] vector) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.toString();
    }
}
