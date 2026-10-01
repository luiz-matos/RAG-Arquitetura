package br.com.luizmatosdev.ragarquitetura.consulta;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BuscaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BuscadorDeTrechos buscador;

    @Test
    void devolveOsTrechosEncontradosComAFonte() throws Exception {
        when(buscador.buscar("What is a channel in Asterisk?"))
                .thenReturn(List.of(new TrechoEncontrado(
                        0.906,
                        "The Architecture of Open Source Applications, Volume 1",
                        "Asterisk",
                        "Russell Bryant",
                        "https://aosabook.org/en/v1/asterisk.html",
                        23,
                        "As discussed earlier, a channel is a fundamental concept in Asterisk.")));

        mvc.perform(get("/api/busca").param("pergunta", "What is a channel in Asterisk?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pergunta").value("What is a channel in Asterisk?"))
                .andExpect(jsonPath("$.trechos[0].nota").value(0.906))
                .andExpect(jsonPath("$.trechos[0].capitulo").value("Asterisk"))
                .andExpect(jsonPath("$.trechos[0].url").value("https://aosabook.org/en/v1/asterisk.html"))
                .andExpect(jsonPath("$.trechos[0].posicao").value(23));
    }

    @Test
    void recusaPerguntaVazia() throws Exception {
        mvc.perform(get("/api/busca").param("pergunta", "  ")).andExpect(status().isBadRequest());

        verify(buscador, never()).buscar(anyString());
    }

    @Test
    void recusaRequisicaoSemPergunta() throws Exception {
        mvc.perform(get("/api/busca")).andExpect(status().isBadRequest());
    }
}
