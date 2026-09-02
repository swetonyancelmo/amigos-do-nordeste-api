package br.org.amigosdonordeste.cadastro.comum;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Serve para monitoramento e para acordar a API antes da usuaria precisar dela.
 * Em plano gratuito o servico hiberna; um ping aqui as 6h da manha evita que a
 * primeira requisicao do dia dela seja a que espera o servidor subir.
 * Ver docs/decisoes/ADR-0004.
 */
@Tag(name = "saude")
@RestController
@RequestMapping("/api/saude")
public class SaudeController {

    @GetMapping
    public Map<String, Object> estado() {
        return Map.of("ok", true, "em", OffsetDateTime.now().toString());
    }
}
