package br.com.luizmatosdev.ragarquitetura.ingestao;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * Corta as páginas do livro em trechos de tamanho máximo fixo: a etapa base da pesquisa.
 *
 * <p>O corte tenta, nessa ordem, parágrafo, linha, frase e palavra. Um parágrafo só é cortado ao
 * meio quando sozinho passa do tamanho máximo.
 */
public class CortadorDeTrechos {

    // Uns 300 tokens: 5 trechos e a pergunta cabem no contexto de 4.096 tokens do modelo na CPU
    static final int TAMANHO_MAXIMO = 1200;

    // Repete o fim do trecho anterior, para uma ideia cortada ao meio aparecer inteira em um deles
    static final int SOBREPOSICAO = 120;

    private final DocumentSplitter divisor = DocumentSplitters.recursive(TAMANHO_MAXIMO, SOBREPOSICAO);

    public List<TextSegment> cortar(List<Document> paginas) {
        return divisor.splitAll(paginas);
    }
}
