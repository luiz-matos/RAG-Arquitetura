package br.com.luizmatosdev.ragarquitetura.vetor;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GeradorDeVetoresTest {

    private final ModeloDeEmbeddingFalso modelo = new ModeloDeEmbeddingFalso();
    private final GeradorDeVetores gerador = new GeradorDeVetores(modelo);

    @Test
    void geraUmVetorPorTrechoNaMesmaOrdem() {
        List<Embedding> vetores = gerador.vetorizarTrechos(trechos(70));

        assertThat(vetores).hasSize(70);
        assertThat(vetores.get(0).vector()[1]).isEqualTo(0f);
        assertThat(vetores.get(69).vector()[1]).isEqualTo(69f);
    }

    @Test
    void mandaOsTrechosEmLotes() {
        gerador.vetorizarTrechos(trechos(70));

        assertThat(modelo.chamadas).extracting(List::size).containsExactly(32, 32, 6);
    }

    @Test
    void marcaOTrechoComoTextoASerBuscado() {
        gerador.vetorizarTrechos(List.of(TextSegment.from("A channel represents a connection.")));

        assertThat(modelo.chamadas.getFirst().getFirst().text())
                .isEqualTo("search_document: A channel represents a connection.");
    }

    @Test
    void marcaAPerguntaComoTextoQueBusca() {
        gerador.vetorizarPergunta("What is a channel?");

        assertThat(modelo.chamadas.getFirst().getFirst().text()).isEqualTo("search_query: What is a channel?");
    }

    private static List<TextSegment> trechos(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> TextSegment.from("trecho " + i))
                .toList();
    }
}
