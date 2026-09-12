package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeCreateRequest;
import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
