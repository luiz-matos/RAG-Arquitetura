package br.com.luizmatosdev.ragarquitetura.consulta;

import br.com.luizmatosdev.ragarquitetura.consulta.GeradorDeRespostas.Resposta;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Responde a pergunta com base nos trechos do livro, citando as fontes. */
@RestController
@RequestMapping("/api/resposta")
public class RespostaController {

    private final GeradorDeRespostas gerador;

    public RespostaController(GeradorDeRespostas gerador) {
        this.gerador = gerador;
    }

    @GetMapping
    public Resposta responder(@RequestParam String pergunta) {
        if (pergunta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pergunta não pode ser vazia");
        }
        return gerador.responder(pergunta);
    }
}
