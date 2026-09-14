package ro.scribemed.backend.knowledge.application;

import java.util.List;

public interface EmbeddingProvider {

    List<Double> embed(String texts);
}
