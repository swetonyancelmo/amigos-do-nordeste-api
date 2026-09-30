package br.org.amigosdonordeste.cadastro.pessoa;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.PessoaResponse;
import br.org.amigosdonordeste.cadastro.pessoa.dto.Vinculos.ComunidadeResumo;
import br.org.amigosdonordeste.cadastro.pessoa.dto.Vinculos.FamiliaResumo;
import br.org.amigosdonordeste.cadastro.pessoa.dto.Vinculos.MunicipioResumo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A pessoa completa (GET/POST/PUT de pessoa): os mesmos campos que a pessoa
 * tem dentro da ficha da família — o mesmo PessoaResponse, desembrulhado no
 * JSON, para não existir um segundo mapeamento —, mais família, comunidade e
 * município.
 */
public record PessoaDetalheResponse(
        @JsonUnwrapped PessoaResponse pessoa,
        FamiliaResumo familia,
        ComunidadeResumo comunidade,
        MunicipioResumo municipio
) {
    public static PessoaDetalheResponse fromEntity(Pessoa pessoa) {
        Familia familia = pessoa.getFamilia();
        Comunidade comunidade = familia.getComunidade();
        return new PessoaDetalheResponse(
                PessoaResponse.fromEntity(pessoa),
                FamiliaResumo.de(familia),
                ComunidadeResumo.de(comunidade),
                MunicipioResumo.de(comunidade.getMunicipio()));
    }
}
