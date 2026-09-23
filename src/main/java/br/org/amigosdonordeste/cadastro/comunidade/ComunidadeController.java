package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeCreateRequest;
import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "comunidades")
@RestController
@RequestMapping("/api/comunidades")
public class ComunidadeController {

  private final ComunidadeService comunidadeService;

  public ComunidadeController(ComunidadeService comunidadeService) {
    this.comunidadeService = comunidadeService;
  }

  @Operation(summary = "Lista as comunidades, com filtro opcional por município")
  @GetMapping
  public List<ComunidadeResponse> listar(
      @Parameter(description = "Filtra pelo município; omitido, lista todas as comunidades")
      @RequestParam(required = false) UUID municipioId) {
    return comunidadeService.listar(municipioId);
  }

  /**
   * A lista que o app da agente baixa na ativacao e guarda para escolher a
   * comunidade offline. So AGENTE, como o envio de pre-cadastro: SegurancaConfig
   * ja restringe, e o @PreAuthorize repete a regra para nao depender so de la.
   */
  @Operation(summary = "Lista enxuta de comunidades para o aparelho da agente",
      description = "Só id, nome e município de cada comunidade — sem líder, telefone nem coordenadas.")
  @GetMapping("/opcoes")
  @PreAuthorize("hasRole('AGENTE')")
  public List<ComunidadeOpcaoResponse> listarOpcoes() {
    return comunidadeService.listarOpcoes();
  }

  @Operation(summary = "Busca uma comunidade pelo ID")
  @GetMapping("/{id}")
  public ComunidadeResponse buscarPorId(@PathVariable UUID id) {
    return comunidadeService.buscarPorId(id);
  }

  @Operation(summary = "Cria uma comunidade")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ComunidadeResponse criar(@Valid @RequestBody ComunidadeCreateRequest request) {
    return comunidadeService.criar(request);
  }

  @Operation(summary = "Edita uma comunidade, incluindo latitude e longitude")
  @PutMapping("/{id}")
  public ComunidadeResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ComunidadeUpdateRequest request) {
    return comunidadeService.atualizar(id, request);
  }
}
