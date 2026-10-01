package br.com.luizmatosdev.ragarquitetura.ingestao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Lê o livro baixado pelo scripts/baixar_aosa.py e devolve o texto limpo de cada página, com os
 * metadados que identificam a fonte.
 */
public class LeitorLivro {

    // Só listas de títulos de artigos: atrapalham a busca e não respondem nada
    private static final Set<String> PAGINAS_IGNORADAS = Set.of("v1/bib1.html", "v2/bib2.html");

    // Moldura do site e marcadores de nota de rodapé, que não são texto do livro
    private static final String FORA_DO_TEXTO = ".titlebox, .banner, sup.footnote";

    // Elementos com o texto do livro; a legenda da figura é um <p> dentro dela
    private static final String BLOCOS = "h2, h3, h4, p, pre, li";

    private final Path pastaLivro;
    private final ObjectMapper json = new ObjectMapper();

    public LeitorLivro(Path pastaLivro) {
        this.pastaLivro = pastaLivro;
    }

    public List<Document> lerTudo() throws IOException {
        Manifesto manifesto =
                json.readValue(pastaLivro.resolve("manifesto.json").toFile(), Manifesto.class);
        List<Document> paginas = new ArrayList<>();
        for (PaginaBaixada pagina : manifesto.paginas()) {
            if (PAGINAS_IGNORADAS.contains(pagina.arquivo())) {
                continue;
            }
            String html = Files.readString(pastaLivro.resolve(pagina.arquivo()));
            paginas.add(ler(html, pagina));
        }
        return paginas;
    }

    static Document ler(String html, PaginaBaixada pagina) {
        Element corpo = Jsoup.parse(html).body();
        Metadata metadados = new Metadata()
                .put("livro", pagina.livro())
                .put("arquivo", pagina.arquivo())
                .put("url", pagina.url());

        // O <h1> traz "nome do livro<br>nome do capítulo"
        Element titulo = corpo.selectFirst(".titlebox h1");
        if (titulo != null) {
            metadados.put("capitulo", titulo.textNodes().getLast().text().strip());
        }
        Element autor = corpo.selectFirst(".titlebox .author");
        if (autor != null && !autor.text().isBlank()) {
            metadados.put("autor", autor.text());
        }

        corpo.select(FORA_DO_TEXTO).remove();
        return Document.from(textoDosBlocos(corpo), metadados);
    }

    private static String textoDosBlocos(Element corpo) {
        StringJoiner texto = new StringJoiner("\n\n");
        for (Element bloco : corpo.select(BLOCOS)) {
            // Um <p> dentro de um <li> já entrou pelo texto do <li>
            if (bloco.parents().is(BLOCOS)) {
                continue;
            }
            // Código mantém as quebras de linha; o resto vira uma linha por bloco
            String conteudo = bloco.is("pre") ? bloco.wholeText().strip() : bloco.text();
            if (!conteudo.isBlank()) {
                texto.add(conteudo);
            }
        }
        return texto.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Manifesto(List<PaginaBaixada> paginas) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaginaBaixada(String livro, String arquivo, String url) {}
}
