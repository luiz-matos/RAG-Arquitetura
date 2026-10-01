package br.com.luizmatosdev.ragarquitetura.ingestao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.luizmatosdev.ragarquitetura.vetor.GeradorDeVetores;
import br.com.luizmatosdev.ragarquitetura.vetor.ModeloDeEmbeddingFalso;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// O banco aqui é o store em memória do LangChain4j: mesma interface do PgVectorEmbeddingStore
class IngestaoDoLivroTest {

    private final InMemoryEmbeddingStore<TextSegment> banco = new InMemoryEmbeddingStore<>();
    private IngestaoDoLivro ingestao;

    @BeforeEach
    void montar() throws Exception {
        Path pastaLivro = Path.of(getClass().getResource("/livro").toURI());
        ingestao = new IngestaoDoLivro(
                new LeitorLivro(pastaLivro),
                new CortadorDeTrechos(),
                new GeradorDeVetores(new ModeloDeEmbeddingFalso()),
                banco);
    }

    @Test
    void gravaTodosOsTrechosComAFonteEOModeloQueGerouOVetor() throws Exception {
        int gravados = ingestao.executar();

        assertThat(gravados).isPositive();
        assertThat(tudoQueEstaNoBanco()).hasSize(gravados).allSatisfy(m -> {
            assertThat(m.embedded().metadata().getString("capitulo")).isEqualTo("Asterisk");
            assertThat(m.embedded().metadata().getString("modelo_embedding")).isEqualTo("modelo-falso");
        });
    }

    @Test
    void refazOIndiceDoZeroACadaExecucao() throws Exception {
        int gravados = ingestao.executar();
        ingestao.executar();

        assertThat(tudoQueEstaNoBanco()).hasSize(gravados);
    }

    private List<EmbeddingMatch<TextSegment>> tudoQueEstaNoBanco() {
        return banco.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(Embedding.from(new float[] {1, 0}))
                        .maxResults(1000)
                        .minScore(0.0)
                        .build())
                .matches();
    }
}
