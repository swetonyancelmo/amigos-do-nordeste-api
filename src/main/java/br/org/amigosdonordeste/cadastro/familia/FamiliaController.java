package br.org.amigosdonordeste.cadastro.familia;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.amigosdonordeste.cadastro.familia.request.FamiliaRequest;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Cadastra uma família com membros e fontes de renda numa única chamada")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamiliaResponse criar(@Valid @RequestBody FamiliaRequest request) {
        return familiaService.criar(request);
    }

    @Operation(summary = "Atualiza uma família existente, com merge de membros e fontes de renda")
    @PutMapping("/{id}")
    public FamiliaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody FamiliaRequest request) {
        return familiaService.atualizar(id, request);
    }
}