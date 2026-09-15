package br.org.amigosdonordeste.cadastro.familia.request;

import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * id null = pessoa nova. No PUT, id preenchido = atualiza a existente.
 * No POST, se vier preenchido mesmo numa pessoa nova, serve só de chave
 * temporária pra fontesRenda[].pessoaId apontar pra ela dentro do mesmo
 * payload (ver FamiliaService).
 */
public record PessoaRequest(
        UUID id,
        // pode vir em branco (RF-09: cadastro incompleto, ex. "filha de Jane" sem nome)
        @Size(max = 120) String nome,
        Boolean cadastroIncompleto,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idadeEstimada,
        LocalDate idadeEstimadaEm,
        Parentesco parentesco,
        Boolean estuda,
        @Size(max = 40) String serie,
        TamanhoRoupa tamanhoRoupa,
        // validado contra NumerosCalcado.VALORES no FamiliaService
        String numeroCalcado,
        Boolean gestante,
        String observacoes
) {
}