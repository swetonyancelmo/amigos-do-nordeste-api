package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * O que o motor sabe de uma família: só os campos que alguma sentinela lê.
 * Nada de nome, CPF ou telefone, e nada de entidade JPA: o motor é puro e se
 * testa sem banco. Quem monta isto a partir da entidade é
 * AvaliacaoVulnerabilidadeService.
 *
 * null em qualquer campo é "não informado", nunca "não tem".
 */
public record FatosFamilia(
        Integer numeroComodos,
        Boolean temBanheiro,
        EscoamentoSanitario escoamentoSanitario,
        TratamentoAgua tratamentoAgua,
        Set<AbastecimentoAgua> abastecimentoAgua,
        FaixaRenda faixaRenda,
        List<TipoFonteRenda> fontesRenda,
        List<Pessoa> pessoas
) {
    public FatosFamilia {
        abastecimentoAgua = abastecimentoAgua == null ? Set.of() : Set.copyOf(abastecimentoAgua);
        fontesRenda = fontesRenda == null ? List.of() : List.copyOf(fontesRenda);
        pessoas = pessoas == null ? List.of() : List.copyOf(pessoas);
    }

    /**
     * Um morador. idade em anos completos (dominio.Idade), null quando não há
     * data de nascimento nem estimativa. dataNascimento à parte porque só ela
     * enxerga meses: a idade estimada é em anos.
     */
    public record Pessoa(LocalDate dataNascimento, Integer idade) { }
}
