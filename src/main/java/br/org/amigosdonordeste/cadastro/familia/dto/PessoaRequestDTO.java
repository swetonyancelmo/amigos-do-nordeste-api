package br.org.amigosdonordeste.cadastro.familia.dto;

import br.org.amigosdonordeste.cadastro.pessoa.enuns.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enuns.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enuns.TamanhoRoupa;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/**
 * id: null = pessoa nova.
 * No PUT, id preenchido = atualiza a pessoa existente.
 * No POST, se vier preenchido mesmo numa pessoa nova, serve só como uma
 * "chave temporária" pra fontesRenda[].pessoaId conseguir apontar pra essa
 * pessoa dentro do mesmo payload (ela ainda não tem id de banco). Ver
 * FamiliaService.
 */
public record PessoaRequestDTO(
        UUID id,

        @NotBlank
        String nome,

        Boolean cadastroIncompleto,

        Sexo sexo,

        LocalDate dataNascimento,

        Integer idadeEstimada,

        // se vier null e idadeEstimada vier preenchido, o service usa a data
        // de hoje (regra da issue #14)
        LocalDate idadeEstimadaEm,

        Parentesco parentesco,

        Boolean estuda,

        String serie,

        TamanhoRoupa tamanhoRoupa,

        // TODO: hoje o campo na entidade Pessoa é Integer. Ver aviso no topo
        // da resposta sobre trocar pra String antes da migration.
        String numeroCalcado,

        Boolean gestante,

        String observacoes
) {
}