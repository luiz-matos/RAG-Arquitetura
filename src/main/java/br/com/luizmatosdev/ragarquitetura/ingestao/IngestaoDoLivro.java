package br.com.luizmatosdev.ragarquitetura.ingestao;

import br.com.luizmatosdev.ragarquitetura.vetor.GeradorDeVetores;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O fluxo de ingestão inteiro: lê o livro, corta em trechos, gera os vetores e grava no banco. Cada
 * execução refaz o índice do zero.
 */
public class IngestaoDoLivro {

    private static final Logger log = LoggerFactory.getLogger(IngestaoDoLivro.class);

    private final LeitorLivro leitor;
    private final CortadorDeTrechos cortador;
    private final GeradorDeVetores gerador;
    private final EmbeddingStore<TextSegment> banco;

    public IngestaoDoLivro(
            LeitorLivro leitor,
            CortadorDeTrechos cortador,
            GeradorDeVetores gerador,
            EmbeddingStore<TextSegment> banco) {
        this.leitor = leitor;
        this.cortador = cortador;
        this.gerador = gerador;
        this.banco = banco;
    }

    /** Devolve quantos trechos foram gravados. */
    public int executar() throws IOException {
        List<Document> paginas = leitor.lerTudo();
        List<TextSegment> trechos = cortador.cortar(paginas);
        log.info("{} páginas cortadas em {} trechos", paginas.size(), trechos.size());

        // Vetores de modelos diferentes não se comparam: cada trecho guarda o modelo que gerou o seu
        trechos.forEach(t -> t.metadata().put("modelo_embedding", gerador.nomeDoModelo()));
        List<Embedding> vetores = gerador.vetorizarTrechos(trechos);

        // O índice antigo só sai quando os vetores novos estão prontos: se a vetorização falhar no
        // meio, o banco continua com a versão anterior
        banco.removeAll();
        banco.addAll(vetores, trechos);
        log.info("{} trechos gravados no banco", trechos.size());
        return trechos.size();
    }
}
