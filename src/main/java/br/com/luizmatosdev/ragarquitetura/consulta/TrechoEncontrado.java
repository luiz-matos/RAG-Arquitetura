package br.com.luizmatosdev.ragarquitetura.consulta;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

/**
 * Trecho que a busca achou, com a fonte para citar na resposta.
 *
 * @param nota proximidade com a pergunta, de 0 a 1: (1 + similaridade de cosseno) / 2
 */
public record TrechoEncontrado(
        double nota, String livro, String capitulo, String autor, String url, int posicao, String texto) {

    static TrechoEncontrado de(EmbeddingMatch<TextSegment> encontrado) {
        Metadata fonte = encontrado.embedded().metadata();
        return new TrechoEncontrado(
                encontrado.score(),
                fonte.getString("livro"),
                fonte.getString("capitulo"),
                fonte.getString("autor"),
                fonte.getString("url"),
                Integer.parseInt(fonte.getString("index")),
                encontrado.embedded().text());
    }
}
