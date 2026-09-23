package br.org.amigosdonordeste.cadastro.municipio.dto;

import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import java.util.UUID;

public record MunicipioResposta(UUID id, String nome, String uf, String codigoIbge) {

    /** Converte a entidade no DTO. Um lugar só, usado por todas as rotas. */
    public static MunicipioResposta de(Municipio m) {
        return new MunicipioResposta(m.getId(), m.getNome(), m.getUf(), m.getCodigoIbge());
    }
}
