package br.com.luizmatosdev.ragarquitetura.inspecao;

import br.com.luizmatosdev.ragarquitetura.ingestao.CortadorDeTrechos;
import br.com.luizmatosdev.ragarquitetura.ingestao.LeitorLivro;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Roda a ingestão até onde ela está pronta e mostra o resultado: um resumo no console e o texto
 * em arquivos, para abrir e conferir. Não grava nada no banco.
 *
 * <p>Roda pela IDE, com a raiz do projeto como pasta de trabalho, depois do
 * scripts/baixar_aosa.py.
 */
public class InspecionarIngestao {

    private static final Path PASTA_LIVRO = Path.of("data/raw");
    private static final Path PASTA_TEXTO = Path.of("data/texto");
    private static final Path PASTA_TRECHOS = Path.of("data/trechos");

    public static void main(String[] args) throws IOException {
        List<Document> paginas = new LeitorLivro(PASTA_LIVRO).lerTudo();
        resumirPaginas(paginas);
        gravarPaginas(paginas);

        List<TextSegment> trechos = new CortadorDeTrechos().cortar(paginas);
        resumirTrechos(trechos);
        gravarTrechos(trechos);
    }

    private static void resumirPaginas(List<Document> paginas) {
        System.out.println("== Leitura do livro");
        System.out.printf("%d páginas, %,d caracteres%n", paginas.size(), totalDeCaracteres(paginas));

        Map<String, List<Document>> porLivro = paginas.stream()
                .collect(
                        Collectors.groupingBy(p -> p.metadata().getString("livro"), TreeMap::new, Collectors.toList()));
        porLivro.forEach((livro, doLivro) -> System.out.printf(
                "  %-55s %2d páginas, %,9d caracteres%n", livro, doLivro.size(), totalDeCaracteres(doLivro)));

        Document primeira = paginas.getFirst();
        System.out.printf("%nExemplo: %s%n%s%n", primeira.metadata().toMap(), inicio(primeira.text(), 500));
    }

    // Um .txt por página, no mesmo caminho do HTML: data/raw/v1/asterisk.html vira data/texto/v1/asterisk.txt
    private static void gravarPaginas(List<Document> paginas) throws IOException {
        for (Document pagina : paginas) {
            String arquivo = pagina.metadata().getString("arquivo").replace(".html", ".txt");
            Path destino = PASTA_TEXTO.resolve(arquivo);
            Files.createDirectories(destino.getParent());
            Files.writeString(destino, cabecalho(pagina) + pagina.text());
        }
        System.out.printf("%nTexto de cada página em %s%n", PASTA_TEXTO.toAbsolutePath());
    }

    private static void resumirTrechos(List<TextSegment> trechos) {
        IntSummaryStatistics tamanhos =
                trechos.stream().mapToInt(t -> t.text().length()).summaryStatistics();
        System.out.printf("%n== Corte em trechos%n");
        System.out.printf(
                "%,d trechos; tamanho mínimo %d, médio %d, máximo %d caracteres%n",
                trechos.size(), tamanhos.getMin(), (int) tamanhos.getAverage(), tamanhos.getMax());
    }

    // Um .txt por página com os trechos dela em ordem: data/trechos/v1/asterisk.txt
    private static void gravarTrechos(List<TextSegment> trechos) throws IOException {
        Map<String, List<TextSegment>> porPagina =
                trechos.stream().collect(Collectors.groupingBy(t -> t.metadata().getString("arquivo")));
        for (var pagina : porPagina.entrySet()) {
            Path destino = PASTA_TRECHOS.resolve(pagina.getKey().replace(".html", ".txt"));
            Files.createDirectories(destino.getParent());
            String conteudo = pagina.getValue().stream()
                    .map(t -> "===== trecho %s (%d caracteres)%n%s"
                            .formatted(t.metadata().getString("index"), t.text().length(), t.text()))
                    .collect(Collectors.joining("\n\n"));
            Files.writeString(destino, conteudo);
        }
        System.out.printf("Trechos de cada página em %s%n", PASTA_TRECHOS.toAbsolutePath());
    }

    private static String cabecalho(Document documento) {
        return new TreeMap<>(documento.metadata().toMap())
                        .entrySet().stream()
                                .map(e -> e.getKey() + ": " + e.getValue())
                                .collect(Collectors.joining("\n"))
                + "\n\n";
    }

    private static long totalDeCaracteres(List<Document> documentos) {
        return documentos.stream().mapToLong(d -> d.text().length()).sum();
    }

    private static String inicio(String texto, int tamanho) {
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho) + "...";
    }
}
