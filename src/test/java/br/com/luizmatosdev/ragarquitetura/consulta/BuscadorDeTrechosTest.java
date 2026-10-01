package br.com.luizmatosdev.ragarquitetura.consulta;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.luizmatosdev.ragarquitetura.vetor.GeradorDeVetores;
import br.com.luizmatosdev.ragarquitetura.vetor.ModeloDeEmbeddingFalso;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuscadorDeTrechosTest {

    private final ModeloDeEmbeddingFalso modelo = new ModeloDeEmbeddingFalso();
    private final InMemoryEmbeddingStore<TextSegment> banco = new InMemoryEmbeddingStore<>();

    // O modelo falso devolve [1, 0] para a primeira pergunta: o vetor do trecho do Asterisk
    @Test
    void devolveOsTrechosDoMaisParaOMenosProximoComAFonte() {
        gravar("A channel represents a connection.", "Asterisk", new float[] {1, 0});
        gravar("nginx uses an event-driven architecture.", "nginx", new float[] {0, 1});
        gravar("Channels are bridged together.", "Asterisk", new float[] {1, 1});

        List<TrechoEncontrado> encontrados =
                new BuscadorDeTrechos(new GeradorDeVetores(modelo), banco, 5).buscar("What is a channel?");

        assertThat(encontrados)
                .extracting(TrechoEncontrado::texto)
                .containsExactly(
                        "A channel represents a connection.",
                        "Channels are bridged together.",
                        "nginx uses an event-driven architecture.");
        TrechoEncontrado primeiro = encontrados.getFirst();
        assertThat(primeiro.nota()).isEqualTo(1.0);
        assertThat(primeiro.capitulo()).isEqualTo("Asterisk");
        assertThat(primeiro.url()).isEqualTo("https://aosabook.org/en/v1/asterisk.html");
        assertThat(primeiro.posicao()).isEqualTo(3);
    }

    @Test
    void devolveNoMaximoAQuantidadeConfigurada() {
        for (int i = 0; i < 10; i++) {
            gravar("trecho " + i, "Asterisk", new float[] {1, i});
        }

        List<TrechoEncontrado> encontrados =
                new BuscadorDeTrechos(new GeradorDeVetores(modelo), banco, 5).buscar("What is a channel?");

        assertThat(encontrados).hasSize(5);
    }

    private void gravar(String texto, String capitulo, float[] vetor) {
        Metadata fonte = new Metadata()
                .put("livro", "The Architecture of Open Source Applications, Volume 1")
                .put("capitulo", capitulo)
                .put("autor", "Russell Bryant")
                .put("url", "https://aosabook.org/en/v1/asterisk.html")
                .put("index", "3");
        banco.add(Embedding.from(vetor), TextSegment.from(texto, fonte));
    }
}
