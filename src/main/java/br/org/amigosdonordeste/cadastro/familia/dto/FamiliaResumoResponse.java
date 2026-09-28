package br.org.amigosdonordeste.cadastro.familia.dto;

import java.util.UUID;

public record FamiliaResumoResponse(
  UUID id,
  String responsavelNome,
  String comunidadeNome,
  String municipioNome,
  boolean semBanheiro,
  int totalPessoas,
  int totalAte12,
  int total13a59,
  int total60OuMais
) {}
