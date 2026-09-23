package br.org.amigosdonordeste.cadastro.familia;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "familias")
@RestController
@RequestMapping("/api/familias")
public class FamiliaController {

    private final FamiliaService familiaService;

    public FamiliaController(FamiliaService familiaService) {
        this.familiaService = familiaService;
    }

    @Operation(summary = "Lista as famílias; inativas ficam de fora, a não ser com incluirInativas=true")
    @GetMapping
    public List<FamiliaResumoResponse> listar(
            @Parameter(description = "true inclui as inativas — é como se acha uma família para reativar")
            @RequestParam(defaultValue = "false") boolean incluirInativas) {
        return familiaService.listar(incluirInativas);
    }

    @Operation(summary = "Ficha completa da família: comunidade, município, membros, fontes de renda e totais")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família encontrada"),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping("/{id}")
    public FamiliaDetalheResponse buscarPorId(@PathVariable UUID id) {
        return familiaService.buscarPorId(id);
    }

    @Operation(summary = "Cadastra uma família com membros e fontes de renda numa única chamada")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamiliaResponse criar(@Valid @RequestBody CriarFamiliaRequisicao request) {
        return familiaService.criar(request);
    }

    @Operation(summary = "Atualiza uma família existente, com merge de membros e fontes de renda")
    @PutMapping("/{id}")
    public FamiliaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarFamiliaRequisicao request) {
        return familiaService.atualizar(id, request);
    }

    @Operation(summary = "Inativa a família: some de listagem, contagem, relatório e mapa, mas não é apagada")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família inativa"),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/{id}/inativar")
    public FamiliaResumoResponse inativar(@PathVariable UUID id) {
        return familiaService.inativar(id);
    }

    @Operation(summary = "Reativa uma família inativa: volta a aparecer e a ser contada")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família ativa"),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/{id}/reativar")
    public FamiliaResumoResponse reativar(@PathVariable UUID id) {
        return familiaService.reativar(id);
    }
}