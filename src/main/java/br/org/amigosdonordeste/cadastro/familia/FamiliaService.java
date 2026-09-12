package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaRequestDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaResponseDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.FonteRendaRequestDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.PessoaRequestDTO;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FamiliaService {

    private final FamiliaRepositorio familiaRepositorio;
    private final ComunidadeRepositorio comunidadeRepositorio;

    public FamiliaService(FamiliaRepositorio familiaRepositorio, ComunidadeRepositorio comunidadeRepositorio) {
        this.familiaRepositorio = familiaRepositorio;
        this.comunidadeRepositorio = comunidadeRepositorio;
    }

    /** Issue #14 — POST /api/familias */
    @Transactional
    public FamiliaResponseDTO criar(FamiliaRequestDTO dto) {
        Familia familia = new Familia();
        familia.setComunidade(buscarComunidade(dto.comunidadeId()));
        aplicarCamposSimples(familia, dto);

        // id-informado-pelo-cliente -> Pessoa, só pra fontesRenda conseguir
        // referenciar uma pessoa nova dentro do mesmo payload.
        Map<UUID, Pessoa> pessoasPorIdDoPayload = new HashMap<>();

        for (PessoaRequestDTO pessoaDTO : dto.pessoas()) {
            Pessoa pessoa = new Pessoa();
            aplicarCamposPessoa(pessoa, pessoaDTO);
            familia.adicionarPessoa(pessoa);
            if (pessoaDTO.id() != null) {
                pessoasPorIdDoPayload.put(pessoaDTO.id(), pessoa);
            }
        }

        for (FonteRendaRequestDTO fonteDTO : dto.fontesRenda()) {
            FonteRenda fonte = new FonteRenda();
            fonte.setTipo(fonteDTO.tipo());
            fonte.setFaixa(fonteDTO.faixa());
            fonte.setObservacao(fonteDTO.observacao());
            if (fonteDTO.pessoaId() != null) {
                Pessoa pessoaDaFonte = pessoasPorIdDoPayload.get(fonteDTO.pessoaId());
                if (pessoaDaFonte == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "fontesRenda.pessoaId não corresponde a nenhuma pessoa do payload: " + fonteDTO.pessoaId());
                }
                fonte.setPessoa(pessoaDaFonte);
            }
            familia.adicionarFonteRenda(fonte);
        }

        return FamiliaResponseDTO.from(familiaRepositorio.save(familia));
    }

    /** Issue #15 — PUT /api/familias/{id} */
    @Transactional
    public FamiliaResponseDTO atualizar(UUID id, FamiliaRequestDTO dto) {
        Familia familia = familiaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Família não encontrada: " + id));

        familia.setComunidade(buscarComunidade(dto.comunidadeId()));
        aplicarCamposSimples(familia, dto);

        Map<UUID, Pessoa> pessoasAtuaisPorId = new HashMap<>();
        for (Pessoa pessoa : familia.getPessoas()) {
            pessoasAtuaisPorId.put(pessoa.getId(), pessoa);
        }

        Set<UUID> idsQueContinuam = new HashSet<>();
        Map<UUID, Pessoa> pessoasPorIdDoPayload = new HashMap<>();

        for (PessoaRequestDTO pessoaDTO : dto.pessoas()) {
            if (pessoaDTO.id() != null && pessoasAtuaisPorId.containsKey(pessoaDTO.id())) {
                Pessoa existente = pessoasAtuaisPorId.get(pessoaDTO.id());
                aplicarCamposPessoa(existente, pessoaDTO);
                idsQueContinuam.add(existente.getId());
                pessoasPorIdDoPayload.put(pessoaDTO.id(), existente);
            } else {
                Pessoa nova = new Pessoa();
                aplicarCamposPessoa(nova, pessoaDTO);
                familia.adicionarPessoa(nova);
                if (pessoaDTO.id() != null) {
                    pessoasPorIdDoPayload.put(pessoaDTO.id(), nova);
                }
            }
        }

        // pessoas que sumiram do payload: antes de remover, desliga a fonte
        // de renda dela (pessoa_id = null) em vez de deixar o orphanRemoval
        // tentar apagar a fonte também.
        familia.getPessoas().stream()
                .filter(p -> p.getId() != null && !idsQueContinuam.contains(p.getId()))
                .toList()
                .forEach(pessoaRemovida -> {
                    familia.getFontesRenda().stream()
                            .filter(f -> pessoaRemovida.equals(f.getPessoa()))
                            .forEach(f -> f.setPessoa(null));
                    familia.removerPessoa(pessoaRemovida);
                });

        Map<UUID, FonteRenda> fontesAtuaisPorId = new HashMap<>();
        for (FonteRenda fonte : familia.getFontesRenda()) {
            fontesAtuaisPorId.put(fonte.getId(), fonte);
        }
        Set<UUID> idsFontesQueContinuam = new HashSet<>();

        for (FonteRendaRequestDTO fonteDTO : dto.fontesRenda()) {
            Pessoa pessoaDaFonte = null;
            if (fonteDTO.pessoaId() != null) {
                pessoaDaFonte = pessoasPorIdDoPayload.get(fonteDTO.pessoaId());
                if (pessoaDaFonte == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "fontesRenda.pessoaId não corresponde a nenhuma pessoa do payload: " + fonteDTO.pessoaId());
                }
            }

            if (fonteDTO.id() != null && fontesAtuaisPorId.containsKey(fonteDTO.id())) {
                FonteRenda existente = fontesAtuaisPorId.get(fonteDTO.id());
                existente.setTipo(fonteDTO.tipo());
                existente.setFaixa(fonteDTO.faixa());
                existente.setObservacao(fonteDTO.observacao());
                existente.setPessoa(pessoaDaFonte);
                idsFontesQueContinuam.add(existente.getId());
            } else {
                FonteRenda nova = new FonteRenda();
                nova.setTipo(fonteDTO.tipo());
                nova.setFaixa(fonteDTO.faixa());
                nova.setObservacao(fonteDTO.observacao());
                nova.setPessoa(pessoaDaFonte);
                familia.adicionarFonteRenda(nova);
            }
        }

        familia.getFontesRenda().stream()
                .filter(f -> f.getId() != null && !idsFontesQueContinuam.contains(f.getId()))
                .toList()
                .forEach(familia::removerFonteRenda);

        return FamiliaResponseDTO.from(familiaRepositorio.save(familia));
    }

    private Comunidade buscarComunidade(UUID comunidadeId) {
        return comunidadeRepositorio.findById(comunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "comunidadeId inválido: " + comunidadeId));
    }

    private void aplicarCamposSimples(Familia familia, FamiliaRequestDTO dto) {
        familia.setResponsavelNome(dto.responsavelNome());
        familia.setResponsavelCpf(dto.responsavelCpf());
        familia.setTelefone(dto.telefone());
        familia.setPontoReferencia(dto.pontoReferencia());
        familia.setTemBanheiro(dto.temBanheiro());
        familia.setEscoamentoSanitario(dto.escoamentoSanitario());
        familia.setTratamentoAgua(dto.tratamentoAgua());
        familia.setObservacoes(dto.observacoes());

        familia.getAbastecimentoAgua().clear();
        if (dto.abastecimentoAgua() != null) {
            familia.getAbastecimentoAgua().addAll(dto.abastecimentoAgua());
        }
    }

    private void aplicarCamposPessoa(Pessoa pessoa, PessoaRequestDTO dto) {
        pessoa.setNome(dto.nome());
        pessoa.setCadastroIncompleto(Boolean.TRUE.equals(dto.cadastroIncompleto()));
        pessoa.setSexo(dto.sexo());
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoa.setIdadeEstimada(dto.idadeEstimada());

        // Regra da issue #14: se idadeEstimada veio sem idadeEstimadaEm,
        // preenche com a data de hoje no servidor.
        LocalDate dataEstimativa = dto.idadeEstimadaEm();
        if (dto.idadeEstimada() != null && dataEstimativa == null) {
            dataEstimativa = LocalDate.now();
        }
        pessoa.setIdadeEstimadaEm(dataEstimativa);

        pessoa.setParentesco(dto.parentesco());
        pessoa.setEstuda(dto.estuda());
        pessoa.setSerie(dto.serie());
        pessoa.setTamanhoRoupa(dto.tamanhoRoupa());
        pessoa.setNumeroCalcado(converterNumeroCalcado(dto.numeroCalcado()));
        pessoa.setGestante(dto.gestante());
        pessoa.setObservacoes(dto.observacoes());
    }

    /**
     * TODO PROVISÓRIO: Pessoa.numeroCalcado é Integer, mas a issue #14 manda
     * faixa tipo "26/27". Aqui eu só pego o primeiro número e jogo fora o
     * resto ("26/27" -> 26) pra não travar o build. Ver aviso sobre trocar
     * o campo pra String antes da migration da tabela pessoa.
     */
    private Integer converterNumeroCalcado(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String primeiroNumero = valor.split("/")[0].trim();
        try {
            return Integer.valueOf(primeiroNumero);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "numeroCalcado inválido: " + valor);
        }
    }
}