package br.org.amigosdonordeste.cadastro.pessoa.dto;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Família, comunidade e município de uma pessoa, como informação de resposta.
 * Nenhum deles é campo de pessoa: saem de familia → comunidade → município.
 */
public final class Vinculos {

    private Vinculos() { }

    /**
     * Família não tem nome: a tela monta "Família da Joana" a partir de
     * responsavelNome.
     */
    public record FamiliaResumo(
            UUID id,
            @Schema(example = "Joana Exemplo") String responsavelNome
    ) {
        public static FamiliaResumo de(Familia familia) {
            return new FamiliaResumo(familia.getId(), familia.getResponsavelNome());
        }
    }

    public record ComunidadeResumo(UUID id, @Schema(example = "Sítio Exemplo") String nome) {
        public static ComunidadeResumo de(Comunidade comunidade) {
            return new ComunidadeResumo(comunidade.getId(), comunidade.getNome());
        }
    }

    public record MunicipioResumo(UUID id, @Schema(example = "Município Exemplo") String nome) {
        public static MunicipioResumo de(Municipio municipio) {
            return new MunicipioResumo(municipio.getId(), municipio.getNome());
        }
    }
}
