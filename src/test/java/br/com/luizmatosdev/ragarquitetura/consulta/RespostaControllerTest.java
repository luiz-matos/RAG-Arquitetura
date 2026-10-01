package br.com.luizmatosdev.ragarquitetura.consulta;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.luizmatosdev.ragarquitetura.consulta.GeradorDeRespostas.Resposta;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RespostaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GeradorDeRespostas gerador;

    @Test
    void devolveARespostaComAsFontes() throws Exception {
        TrechoEncontrado fonte = new TrechoEncontrado(
                0.907,
                "The Architecture of Open Source Applications, Volume 1",
                "Asterisk",
                "Russell Bryant",
                "https://aosabook.org/en/v1/asterisk.html",
                1,
                "A channel in Asterisk represents a connection.");
        when(gerador.responder("What is a channel in Asterisk?"))
                .thenReturn(new Resposta(
                        "What is a channel in Asterisk?", "Um channel representa uma conexão [1].", List.of(fonte)));

        mvc.perform(get("/api/resposta").param("pergunta", "What is a channel in Asterisk?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pergunta").value("What is a channel in Asterisk?"))
                .andExpect(jsonPath("$.resposta").value("Um channel representa uma conexão [1]."))
                .andExpect(jsonPath("$.fontes[0].capitulo").value("Asterisk"))
                .andExpect(jsonPath("$.fontes[0].url").value("https://aosabook.org/en/v1/asterisk.html"));
    }

    @Test
    void recusaPerguntaVazia() throws Exception {
        mvc.perform(get("/api/resposta").param("pergunta", "  ")).andExpect(status().isBadRequest());

        verify(gerador, never()).responder(anyString());
    }
}
