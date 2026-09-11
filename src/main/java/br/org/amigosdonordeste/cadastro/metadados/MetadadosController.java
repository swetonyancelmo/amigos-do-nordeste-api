package br.org.amigosdonordeste.cadastro.metadados;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadados")
public class MetadadosController {

    private final MetadadosService metadadosService;

    public MetadadosController(MetadadosService metadadosService) {
        this.metadadosService = metadadosService;
    }

    @GetMapping
    public MetadadosResponse listar() {
        return metadadosService.listar();
    }
}