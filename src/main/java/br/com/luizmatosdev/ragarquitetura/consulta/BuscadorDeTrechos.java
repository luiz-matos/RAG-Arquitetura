package br.com.luizmatosdev.ragarquitetura.consulta;

import br.com.luizmatosdev.ragarquitetura.vetor.GeradorDeVetores;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;

/** Acha no banco os trechos de sentido mais próximo da pergunta, do mais para o menos próximo. */
public class BuscadorDeTrechos {

    private final GeradorDeVetores gerador;
    private final EmbeddingStore<TextSegment> banco;
    private final int quantidade;

    public BuscadorDeTrechos(GeradorDeVetores gerador, EmbeddingStore<TextSegment> banco, int quantidade) {
        this.gerador = gerador;
        this.banco = banco;
        this.quantidade = quantidade;
    }

    public List<TrechoEncontrado> buscar(String pergunta) {
        EmbeddingSearchRequest busca = EmbeddingSearchRequest.builder()
                .queryEmbedding(gerador.vetorizarPergunta(pergunta))
                .maxResults(quantidade)
                .build();
        return banco.search(busca).matches().stream().map(TrechoEncontrado::de).toList();
    }
}
