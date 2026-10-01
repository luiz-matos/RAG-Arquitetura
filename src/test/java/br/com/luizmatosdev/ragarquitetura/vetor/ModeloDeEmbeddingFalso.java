package br.com.luizmatosdev.ragarquitetura.vetor;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de embedding para os testes, sem Ollama: guarda o que recebeu e devolve o vetor
 * [1, posição do texto], para conferir a ordem.
 */
public class ModeloDeEmbeddingFalso implements EmbeddingModel {

    public final List<List<TextSegment>> chamadas = new ArrayList<>();
    private int contador = 0;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textos) {
        chamadas.add(textos);
        return Response.from(textos.stream()
                .map(t -> Embedding.from(new float[] {1, contador++}))
                .toList());
    }

    @Override
    public String modelName() {
        return "modelo-falso";
    }
}
