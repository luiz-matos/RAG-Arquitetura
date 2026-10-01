package br.com.luizmatosdev.ragarquitetura.vetor;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GeradorDeVetoresTest {

    private final ModeloFalso modelo = new ModeloFalso();
    private final GeradorDeVetores gerador = new GeradorDeVetores(modelo);

    @Test
    void geraUmVetorPorTrechoNaMesmaOrdem() {
        List<Embedding> vetores = gerador.vetorizarTrechos(trechos(70));

        assertThat(vetores).hasSize(70);
        assertThat(vetores.get(0).vector()[0]).isEqualTo(0f);
        assertThat(vetores.get(69).vector()[0]).isEqualTo(69f);
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

    // Guarda o que recebeu e devolve um vetor com a posição do texto, para conferir a ordem
    private static class ModeloFalso implements EmbeddingModel {

        private final List<List<TextSegment>> chamadas = new ArrayList<>();
        private int contador = 0;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textos) {
            chamadas.add(textos);
            return Response.from(textos.stream()
                    .map(t -> Embedding.from(new float[] {contador++}))
                    .toList());
        }
    }
}
