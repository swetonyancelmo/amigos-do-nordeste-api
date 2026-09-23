package br.org.amigosdonordeste.cadastro.familia;

import java.util.UUID;

/**
 * Uma linha de GET /api/familias. Enxuta de proposito: so o que vem na
 * mesma consulta (familia + comunidade + municipio), sem pessoas nem fontes
 * de renda — carregar essas colecoes aqui seria duas consultas por linha.
 * A issue #16 acrescenta busca, filtros, paginacao e totais.
 */
public record FamiliaResumoResponse(
        UUID id,
        String responsavelNome,
        UUID comunidadeId,
        String comunidadeNome,
        String municipioNome,
        boolean ativa
) {
    public static FamiliaResumoResponse fromEntity(Familia familia) {
        return new FamiliaResumoResponse(
                familia.getId(),
                familia.getResponsavelNome(),
                familia.getComunidade().getId(),
                familia.getComunidade().getNome(),
                familia.getComunidade().getMunicipio().getNome(),
                familia.isAtiva());
    }
}
