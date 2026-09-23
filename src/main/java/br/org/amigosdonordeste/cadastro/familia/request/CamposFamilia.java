package br.org.amigosdonordeste.cadastro.familia.request;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;

import java.util.Set;
import java.util.UUID;

/**
 * O que POST e PUT têm em comum na família. Os records implementam sem
 * escrever nada: os acessores já batem com as assinaturas.
 */
public sealed interface CamposFamilia permits CriarFamiliaRequisicao, AtualizarFamiliaRequisicao {
    UUID comunidadeId();
    String responsavelNome();
    String responsavelCpf();
    String telefone();
    String pontoReferencia();
    Boolean temBanheiro();
    EscoamentoSanitario escoamentoSanitario();
    TratamentoAgua tratamentoAgua();
    Set<AbastecimentoAgua> abastecimentoAgua();
    String observacoes();
}
