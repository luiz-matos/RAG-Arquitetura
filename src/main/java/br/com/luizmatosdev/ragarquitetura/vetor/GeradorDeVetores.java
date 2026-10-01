package br.com.luizmatosdev.ragarquitetura.vetor;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforma texto em vetor com o modelo de embedding: os trechos do livro na ingestão e a
 * pergunta na consulta. Textos de sentido parecido viram vetores próximos, e é essa proximidade
 * que a busca usa.
 */
public class GeradorDeVetores {

    // O nomic-embed-text foi treinado para saber o papel do texto: o que vai ser buscado e o que busca
    static final String PREFIXO_TRECHO = "search_document: ";
    static final String PREFIXO_PERGUNTA = "search_query: ";

    // Trechos por chamada ao Ollama: a lista inteira numa chamada só passaria do tempo limite
    static final int TAMANHO_DO_LOTE = 32;

    private static final Logger log = LoggerFactory.getLogger(GeradorDeVetores.class);

    private final EmbeddingModel modelo;

    public GeradorDeVetores(EmbeddingModel modelo) {
        this.modelo = modelo;
    }

    /** Um vetor por trecho, na mesma ordem da lista recebida. */
    public List<Embedding> vetorizarTrechos(List<TextSegment> trechos) {
        List<Embedding> vetores = new ArrayList<>(trechos.size());
        for (int inicio = 0; inicio < trechos.size(); inicio += TAMANHO_DO_LOTE) {
            List<TextSegment> lote = trechos.subList(inicio, Math.min(inicio + TAMANHO_DO_LOTE, trechos.size()));
            List<TextSegment> comPrefixo = lote.stream()
                    .map(t -> TextSegment.from(PREFIXO_TRECHO + t.text()))
                    .toList();
            vetores.addAll(modelo.embedAll(comPrefixo).content());
            log.info("{} de {} trechos vetorizados", vetores.size(), trechos.size());
        }
        return vetores;
    }

    public Embedding vetorizarPergunta(String pergunta) {
        return modelo.embed(PREFIXO_PERGUNTA + pergunta).content();
    }

    public String nomeDoModelo() {
        return modelo.modelName();
    }
}
