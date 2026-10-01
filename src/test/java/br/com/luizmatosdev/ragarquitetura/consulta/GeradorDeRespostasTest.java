package br.com.luizmatosdev.ragarquitetura.consulta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.luizmatosdev.ragarquitetura.consulta.GeradorDeRespostas.Resposta;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeradorDeRespostasTest {

    private static final List<TrechoEncontrado> TRECHOS = List.of(
            trecho("A channel in Asterisk represents a connection.", 1),
            trecho("Channels are bridged together to pass media.", 3));

    private final BuscadorDeTrechos buscador = mock(BuscadorDeTrechos.class);
    private final ModeloDeConversaFalso modelo = new ModeloDeConversaFalso();
    private final GeradorDeRespostas gerador = new GeradorDeRespostas(buscador, modelo);

    @Test
    void mandaAsInstrucoesEOsTrechosNumeradosComAPergunta() {
        when(buscador.buscar("What is a channel?")).thenReturn(TRECHOS);

        gerador.responder("What is a channel?");

        List<ChatMessage> mensagens = modelo.mensagens.getFirst();
        assertThat(((SystemMessage) mensagens.get(0)).text()).isEqualTo(GeradorDeRespostas.INSTRUCOES);
        assertThat(((UserMessage) mensagens.get(1)).singleText()).isEqualTo("""
                        Trechos:

                        [1] (The Architecture of Open Source Applications, Volume 1, capítulo Asterisk)
                        A channel in Asterisk represents a connection.

                        [2] (The Architecture of Open Source Applications, Volume 1, capítulo Asterisk)
                        Channels are bridged together to pass media.

                        Pergunta: What is a channel?

                        Responda em português do Brasil.""");
    }

    @Test
    void devolveARespostaDoModeloComAsFontesNaOrdemDosNumeros() {
        when(buscador.buscar("What is a channel?")).thenReturn(TRECHOS);

        Resposta resposta = gerador.responder("What is a channel?");

        assertThat(resposta.resposta()).isEqualTo("Um channel é uma conexão [1].");
        assertThat(resposta.fontes()).isEqualTo(TRECHOS);
    }

    @Test
    void semTrechosNaoChamaOModelo() {
        when(buscador.buscar("What is a channel?")).thenReturn(List.of());

        Resposta resposta = gerador.responder("What is a channel?");

        assertThat(resposta.resposta()).isEqualTo(GeradorDeRespostas.NAO_ENCONTREI);
        assertThat(modelo.mensagens).isEmpty();
    }

    private static TrechoEncontrado trecho(String texto, int posicao) {
        return new TrechoEncontrado(
                0.9,
                "The Architecture of Open Source Applications, Volume 1",
                "Asterisk",
                "Russell Bryant",
                "https://aosabook.org/en/v1/asterisk.html",
                posicao,
                texto);
    }

    // Guarda as mensagens recebidas e responde sempre o mesmo texto, com espaços sobrando nas pontas
    private static class ModeloDeConversaFalso implements ChatModel {

        private final List<List<ChatMessage>> mensagens = new ArrayList<>();

        @Override
        public ChatResponse doChat(ChatRequest pedido) {
            mensagens.add(pedido.messages());
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("  Um channel é uma conexão [1].\n"))
                    .build();
        }
    }
}
