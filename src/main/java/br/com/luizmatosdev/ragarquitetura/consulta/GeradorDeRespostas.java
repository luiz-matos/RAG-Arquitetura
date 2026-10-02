package br.com.luizmatosdev.ragarquitetura.consulta;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * O RAG completo: busca os trechos da pergunta, monta o prompt com eles e pede a resposta ao
 * modelo de geração.
 */
public class GeradorDeRespostas {

    static final String NAO_ENCONTREI = "Não encontrei isso nos livros.";

    static final String INSTRUCOES = """
            Você responde perguntas sobre arquitetura de software usando apenas os trechos de livros \
            fornecidos.

            Regras:
            - Responda sempre em português do Brasil, de forma direta, mesmo que a pergunta e os \
            trechos estejam em inglês.
            - Use só a informação dos trechos. Se eles não trazem a resposta, responda apenas: %s
            - Cite os trechos que usou pelo número entre colchetes, como [1] ou [3].
            - Não mencione figuras do livro, como "(Figure 1.1)": quem lê a resposta não vê as figuras.
            - Mantenha em inglês os nomes de sistemas, componentes e termos técnicos.""".formatted(NAO_ENCONTREI);

    // Repetida no fim da mensagem: modelo pequeno segue a língua da pergunta e esquece a instrução do início
    static final String LEMBRETE_DE_LINGUA = "Responda em português do Brasil.";

    private final BuscadorDeTrechos buscador;
    private final ChatModel modelo;

    public GeradorDeRespostas(BuscadorDeTrechos buscador, ChatModel modelo) {
        this.buscador = buscador;
        this.modelo = modelo;
    }

    public Resposta responder(String pergunta) {
        List<TrechoEncontrado> trechos = buscador.buscar(pergunta);
        if (trechos.isEmpty()) {
            return new Resposta(pergunta, NAO_ENCONTREI, trechos);
        }
        String resposta = modelo.chat(SystemMessage.from(INSTRUCOES), UserMessage.from(prompt(pergunta, trechos)))
                .aiMessage()
                .text()
                .strip();
        return new Resposta(pergunta, resposta, trechos);
    }

    // Cada trecho ganha o número que a resposta usa para citar; o número é a posição em "fontes"
    static String prompt(String pergunta, List<TrechoEncontrado> trechos) {
        String numerados = IntStream.range(0, trechos.size())
                .mapToObj(i -> "[%d] (%s, capítulo %s)\n%s"
                        .formatted(
                                i + 1,
                                trechos.get(i).livro(),
                                trechos.get(i).capitulo(),
                                trechos.get(i).texto()))
                .collect(Collectors.joining("\n\n"));
        return "Trechos:\n\n" + numerados + "\n\nPergunta: " + pergunta + "\n\n" + LEMBRETE_DE_LINGUA;
    }

    /**
     * @param fontes os trechos enviados ao modelo, na ordem dos números citados na resposta
     */
    public record Resposta(String pergunta, String resposta, List<TrechoEncontrado> fontes) {}
}
