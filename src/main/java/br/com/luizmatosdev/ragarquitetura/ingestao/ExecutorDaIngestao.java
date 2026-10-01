package br.com.luizmatosdev.ragarquitetura.ingestao;

import br.com.luizmatosdev.ragarquitetura.vetor.GeradorDeVetores;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.nio.file.Path;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/** Roda a ingestão ao subir a aplicação, só quando rag.ingestao.executar=true. */
@Component
@ConditionalOnBooleanProperty("rag.ingestao.executar")
public class ExecutorDaIngestao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExecutorDaIngestao.class);

    private final IngestaoDoLivro ingestao;

    public ExecutorDaIngestao(
            @Value("${rag.livro.pasta}") Path pastaLivro,
            GeradorDeVetores gerador,
            EmbeddingStore<TextSegment> bancoDeTrechos) {
        this.ingestao =
                new IngestaoDoLivro(new LeitorLivro(pastaLivro), new CortadorDeTrechos(), gerador, bancoDeTrechos);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long inicio = System.nanoTime();
        int trechos = ingestao.executar();
        Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);
        log.info(
                "Ingestão concluída: {} trechos em {} min {} s", trechos, duracao.toMinutes(), duracao.toSecondsPart());
    }
}
