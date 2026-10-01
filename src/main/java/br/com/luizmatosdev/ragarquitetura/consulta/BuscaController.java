package br.com.luizmatosdev.ragarquitetura.consulta;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Mostra o que a busca acha para uma pergunta, antes de qualquer resposta do modelo. */
@RestController
@RequestMapping("/api/busca")
public class BuscaController {

    private final BuscadorDeTrechos buscador;

    public BuscaController(BuscadorDeTrechos buscador) {
        this.buscador = buscador;
    }

    @GetMapping
    public ResultadoDaBusca buscar(@RequestParam String pergunta) {
        if (pergunta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pergunta não pode ser vazia");
        }
        return new ResultadoDaBusca(pergunta, buscador.buscar(pergunta));
    }

    public record ResultadoDaBusca(String pergunta, List<TrechoEncontrado> trechos) {}
}
