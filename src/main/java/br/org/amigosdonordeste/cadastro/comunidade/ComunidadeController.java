package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeCreateRequest;
import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadeController {

  private final ComunidadeService comunidadeService;

  public ComunidadeController(ComunidadeService comunidadeService) {
    this.comunidadeService = comunidadeService;
  }

  @GetMapping("/listar")
  public List<ComunidadeResponse> listar (@RequestParam(required = false) UUID municipioId) {
    return comunidadeService.listar(municipioId);
  }
  @GetMapping("/buscarId/{id}")
  public ComunidadeResponse buscarPorId(@PathVariable UUID id) {
    return comunidadeService.buscarPorId(id);
  }

  @PostMapping("/criar")
  @ResponseStatus(HttpStatus.CREATED)
  public ComunidadeResponse criar(@Valid @RequestBody ComunidadeCreateRequest request) {
    return comunidadeService.criar(request);
  }

  @PutMapping("/atualizar/{id}")
  public ComunidadeResponse atualizar(@PathVariable UUID id, @Valid  @RequestBody ComunidadeUpdateRequest request) {
    return comunidadeService.atualizar(id, request);
  }
}
