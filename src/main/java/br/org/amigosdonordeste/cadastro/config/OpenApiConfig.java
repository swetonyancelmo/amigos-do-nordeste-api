package br.org.amigosdonordeste.cadastro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O Swagger nao e enfeite neste projeto: com backend e frontend em
 * repositorios separados, ele e a referencia do contrato entre as duas equipes.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Cadastro de Famílias — Amigos do Nordeste")
                .description("API do sistema de cadastro das famílias atendidas pela associação.")
                .version("0.1.0"))
            .components(new Components().addSecuritySchemes("bearer",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
