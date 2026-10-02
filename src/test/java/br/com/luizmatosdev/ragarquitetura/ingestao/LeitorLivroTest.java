package br.com.luizmatosdev.ragarquitetura.ingestao;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.document.Document;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeitorLivroTest {

    private Document asterisk;
    private List<Document> paginas;

    @BeforeEach
    void lerAmostra() throws Exception {
        Path pastaLivro = Path.of(getClass().getResource("/livro").toURI());
        paginas = new LeitorLivro(pastaLivro).lerTudo();
        asterisk = paginas.getFirst();
    }

    @Test
    void ignoraBibliografia() {
        assertThat(paginas).hasSize(1);
    }

    @Test
    void identificaAFonteNosMetadados() {
        assertThat(asterisk.metadata().getString("livro"))
                .isEqualTo("The Architecture of Open Source Applications, Volume 1");
        assertThat(asterisk.metadata().getString("capitulo")).isEqualTo("Asterisk");
        assertThat(asterisk.metadata().getString("autor")).isEqualTo("Russell Bryant");
        assertThat(asterisk.metadata().getString("arquivo")).isEqualTo("v1/asterisk.html");
        assertThat(asterisk.metadata().getString("url")).isEqualTo("https://aosabook.org/en/v1/asterisk.html");
    }

    @Test
    void tiraAMolduraDoSite() {
        assertThat(asterisk.text())
                .doesNotContain("The Architecture of Open Source Applications")
                .doesNotContain("If you enjoy these books")
                .startsWith("Asterisk is an open source telephony applications platform");
    }

    @Test
    void mantemSecoesLegendaENotas() {
        assertThat(asterisk.text())
                .contains("1.1.1. Channels")
                .contains("Figure 1.1: A Single Call Leg, Represented by a Single Channel")
                .contains("DTMF stands for Dual-Tone Multi-Frequency.");
    }

    @Test
    void tiraAReferenciaAFiguraDoTexto() {
        assertThat(asterisk.text()).contains("some telephony endpoint.").doesNotContain("(Figure 1.1)");
    }

    // Formatos encontrados no livro
    @Test
    void tiraAReferenciaAFiguraEmTodosOsFormatos() {
        String html = """
                <body>
                <p>One (see Figure 10.3). Two (See Figure 1.11.) Three (Figure 3.2.). Four \
                (Figure 17.7 and Figure 17.8). Five (as shown in Figure 6.4).</p>
                </body>
                """;

        Document documento = LeitorLivro.ler(html, paginaDeTeste());

        assertThat(documento.text()).isEqualTo("One. Two Three. Four. Five.");
    }

    @Test
    void mantemParentesesComInformacaoECodigo() {
        String html = """
                <body>
                <p>The time (requestStart in Figure 1.1) is recorded.</p>
                <pre><code>draw(SnowFigure, self) (Figure 2.1)</code></pre>
                </body>
                """;

        Document documento = LeitorLivro.ler(html, paginaDeTeste());

        assertThat(documento.text())
                .isEqualTo("The time (requestStart in Figure 1.1) is recorded.\n\ndraw(SnowFigure, self) (Figure 2.1)");
    }

    @Test
    void separaOsBlocosPorLinhaEmBranco() {
        assertThat(asterisk.text())
                .contains("phone calls.\n\n1.1. Critical Architectural Concepts\n\nThis section discusses");
    }

    @Test
    void codigoMantemAsQuebrasDeLinha() {
        String html = """
                <body>
                <p>For purposes of illustration:</p>
                <pre class="sourceCode html"><code><span class="kw">&lt;p&gt;</span>Welcome, Charlie!<span class="kw">&lt;/p&gt;</span>
                <span class="kw">&lt;ul&gt;</span></code></pre>
                </body>
                """;
        Document documento = LeitorLivro.ler(html, paginaDeTeste());

        assertThat(documento.text()).isEqualTo("For purposes of illustration:\n\n<p>Welcome, Charlie!</p>\n<ul>");
    }

    private static LeitorLivro.PaginaBaixada paginaDeTeste() {
        return new LeitorLivro.PaginaBaixada("500 Lines or Less", "500L/teste.html", "https://aosabook.org");
    }
}
