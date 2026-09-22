package br.org.amigosdonordeste.cadastro.municipio;

import br.org.amigosdonordeste.cadastro.municipio.dto.MunicipioRequisicao;
import br.org.amigosdonordeste.cadastro.municipio.dto.MunicipioResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Municípios atendidos pela associação.
 *
 * O município é o filtro que move o dashboard inteiro: trocar o município
 * recarrega indicadores, lista de famílias, relatórios e mapa.
 */
@Tag(name = "municipios")
@RestController
@RequestMapping("/api/municipios")
public class MunicipioController {

    private final MunicipioService service;

    public MunicipioController(MunicipioService service) {
        this.service = service;
    }

    @Operation(summary = "Lista os municípios atendidos, em ordem alfabética")
    @GetMapping
    public List<MunicipioResposta> listar() {
        return service.listar().stream().map(MunicipioResposta::de).toList();
    }

    @Operation(summary = "Cadastra um município")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MunicipioResposta criar(@Valid @RequestBody MunicipioRequisicao dados) {
        return MunicipioResposta.de(service.criar(dados));
    }

    @Operation(summary = "Edita um município")
    @PutMapping("/{id}")
    public MunicipioResposta atualizar(@PathVariable UUID id,
                                       @Valid @RequestBody MunicipioRequisicao dados) {
        return MunicipioResposta.de(service.atualizar(id, dados));
    }
}
