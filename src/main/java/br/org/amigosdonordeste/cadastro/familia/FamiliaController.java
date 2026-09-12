package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaRequestDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/familias")
public class FamiliaController {

    private final FamiliaService familiaService;

    public FamiliaController(FamiliaService familiaService) {
        this.familiaService = familiaService;
    }

    // Issue #14
    @PostMapping
    public ResponseEntity<FamiliaResponseDTO> criar(@Valid @RequestBody FamiliaRequestDTO dto) {
        FamiliaResponseDTO criada = familiaService.criar(dto);
        return ResponseEntity.created(URI.create("/api/familias/" + criada.id())).body(criada);
    }

    // Issue #15
    @PutMapping("/{id}")
    public ResponseEntity<FamiliaResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody FamiliaRequestDTO dto) {
        return ResponseEntity.ok(familiaService.atualizar(id, dto));
    }
}