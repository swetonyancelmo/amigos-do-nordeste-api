package br.org.amigosdonordeste.cadastro.comunidade;

import java.util.UUID;

/**
 * Comunidade como opcao de escolha no aparelho da agente: so o que a tela do
 * app mostra ("Sitio Igrejinha · Petrolandia"). Lider, telefone do lider e
 * coordenadas ficam de fora de proposito — o aparelho pode ser perdido, e a
 * lista fica guardada nele.
 */
public record ComunidadeOpcaoResponse(
    UUID id,
    String nome,
    UUID municipioId,
    String municipioNome
) {
    public static ComunidadeOpcaoResponse fromEntity(Comunidade comunidade) {
        return new ComunidadeOpcaoResponse(
            comunidade.getId(),
            comunidade.getNome(),
            comunidade.getMunicipio().getId(),
            comunidade.getMunicipio().getNome());
    }
}
