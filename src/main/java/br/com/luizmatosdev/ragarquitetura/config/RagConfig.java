package br.com.luizmatosdev.ragarquitetura.config;

import br.com.luizmatosdev.ragarquitetura.consulta.BuscadorDeTrechos;
import br.com.luizmatosdev.ragarquitetura.vetor.GeradorDeVetores;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/** Liga as peças do RAG ao Ollama e ao PostgreSQL do docker-compose.yml. */
@Configuration
public class RagConfig {

    @Bean
    EmbeddingModel modeloDeEmbedding(
            @Value("${rag.ollama.url}") String urlOllama, @Value("${rag.embedding.modelo}") String modelo) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(urlOllama)
                .modelName(modelo)
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    GeradorDeVetores geradorDeVetores(EmbeddingModel modeloDeEmbedding) {
        return new GeradorDeVetores(modeloDeEmbedding);
    }

    // @Lazy no parâmetro: o buscador recebe um intermediário, e o store só é montado na primeira busca
    @Bean
    BuscadorDeTrechos buscadorDeTrechos(
            GeradorDeVetores geradorDeVetores,
            @Lazy EmbeddingStore<TextSegment> bancoDeTrechos,
            @Value("${rag.busca.quantidade}") int quantidade) {
        return new BuscadorDeTrechos(geradorDeVetores, bancoDeTrechos, quantidade);
    }

    // Lazy: ao ser montado, o store conecta no banco e cria a tabela, então só nasce quando alguém usa
    @Bean
    @Lazy
    EmbeddingStore<TextSegment> bancoDeTrechos(
            DataSource dataSource, @Value("${rag.embedding.dimensao}") int dimensao) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("trecho")
                .dimension(dimensao)
                .createTable(true)
                // JSONB em vez de JSON: o PostgreSQL consegue indexar e filtrar pelos metadados
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(List.of("metadata JSONB NULL"))
                        .build())
                .build();
    }
}
