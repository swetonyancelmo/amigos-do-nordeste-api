package br.org.amigosdonordeste.cadastro.familia;

import java.util.UUID;

import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaResumoResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao;
import io.swagger.v3.oas.annotations.Operation;
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
  @GetMapping
  public ResponseEntity<Page<FamiliaResumoResponse>> listar(@ModelAttribute FamiliaFiltroDTO filtro) {
    return ResponseEntity.ok(familiaService.listar(filtro));
  }
}
