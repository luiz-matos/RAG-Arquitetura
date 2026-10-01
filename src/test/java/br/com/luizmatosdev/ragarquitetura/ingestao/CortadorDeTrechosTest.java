package br.com.luizmatosdev.ragarquitetura.ingestao;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CortadorDeTrechosTest {

    private final CortadorDeTrechos cortador = new CortadorDeTrechos();

    @Test
    void nenhumTrechoPassaDoTamanhoMaximo() {
        List<TextSegment> trechos = cortador.cortar(List.of(paginaLonga()));

        assertThat(trechos).hasSizeGreaterThan(1);
        assertThat(trechos)
                .allSatisfy(t -> assertThat(t.text()).hasSizeLessThanOrEqualTo(CortadorDeTrechos.TAMANHO_MAXIMO));
    }

    @Test
    void trechoHerdaOsMetadadosDaPaginaENumeraAPosicao() {
        List<TextSegment> trechos = cortador.cortar(List.of(paginaLonga()));

        assertThat(trechos).allSatisfy(t -> {
            assertThat(t.metadata().getString("capitulo")).isEqualTo("Asterisk");
            assertThat(t.metadata().getString("url")).isEqualTo("https://aosabook.org/en/v1/asterisk.html");
        });
        assertThat(trechos.get(0).metadata().getString("index")).isEqualTo("0");
        assertThat(trechos.get(1).metadata().getString("index")).isEqualTo("1");
    }

    // A sobreposição são as frases inteiras do fim do trecho anterior que cabem nos 120 caracteres.
    // O LangChain4j junta as frases repetidas com linha em branco, então o espaçamento é ignorado.
    @Test
    void trechoSeguinteRepeteOFimDoAnterior() {
        List<TextSegment> trechos = cortador.cortar(List.of(paginaLonga()));
        String anterior = trechos.get(0).text();
        String seguinte = trechos.get(1).text();
        String primeiraFraseDoSeguinte = seguinte.substring(0, seguinte.indexOf('.') + 1);
        String fimRepetido = anterior.substring(anterior.lastIndexOf(primeiraFraseDoSeguinte));

        assertThat(semEspacamento(seguinte)).startsWith(semEspacamento(fimRepetido));
        assertThat(fimRepetido).hasSizeLessThanOrEqualTo(CortadorDeTrechos.SOBREPOSICAO);
    }

    private static String semEspacamento(String texto) {
        return texto.replaceAll("\\s+", " ");
    }

    @Test
    void paginaCurtaViraUmTrechoSo() {
        Document pagina = Document.from("A channel in Asterisk represents a connection.", metadados());

        List<TextSegment> trechos = cortador.cortar(List.of(pagina));

        assertThat(trechos).singleElement().extracting(TextSegment::text).isEqualTo(pagina.text());
    }

    // 40 parágrafos de 5 frases, uns 9 mil caracteres
    private static Document paginaLonga() {
        String texto = IntStream.rangeClosed(1, 40)
                .mapToObj(p -> IntStream.rangeClosed(1, 5)
                        .mapToObj(f -> "Paragraph %d, sentence %d, talks about channels.".formatted(p, f))
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining("\n\n"));
        return Document.from(texto, metadados());
    }

    private static Metadata metadados() {
        return new Metadata().put("capitulo", "Asterisk").put("url", "https://aosabook.org/en/v1/asterisk.html");
    }
}
