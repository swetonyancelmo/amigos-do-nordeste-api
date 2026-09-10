package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.enums.TipoComunidade;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeCreateRequest;
import br.org.amigosdonordeste.cadastro.comunidade.request.ComunidadeUpdateRequest;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ComunidadeService {

  private final ComunidadeRepositorio comunidadeRepositorio;
  private final MunicipioRepositorio municipioRepositorio;

  public ComunidadeService(ComunidadeRepositorio comunidadeRepositorio, MunicipioRepositorio municipioRepositorio) {
    this.comunidadeRepositorio = comunidadeRepositorio;
    this.municipioRepositorio = municipioRepositorio;
  }

  private Municipio buscarMunicipio(UUID municipioId) {
    return municipioRepositorio.findById(municipioId)
      .orElseThrow(() -> new MunicipioNaoEncontradoException(municipioId));
  }

  @Transactional(readOnly = true)
  public List<ComunidadeResponse> listar(UUID municipioId){
    List<Comunidade> comunidades = municipioId != null
      ? comunidadeRepositorio.findByMunicipioIdOrderByNomeAsc(municipioId)
      : comunidadeRepositorio.listarComMunicipio();

    return comunidades.stream()
      .map(ComunidadeResponse::fromEntity)
      .toList();
  }
  @Transactional(readOnly = true)
  public ComunidadeResponse buscarPorId(UUID id){
    return comunidadeRepositorio.findById(id)
      .map(ComunidadeResponse::fromEntity)
      .orElseThrow(() -> new ComunidadeNaoEncontradaException(id));
  }

  public ComunidadeResponse criar(ComunidadeCreateRequest request) {
    Municipio municipio = buscarMunicipio(request.municipioId());

    Comunidade comunidade = Comunidade.builder()
      .nome(request.nome())
      .municipio(municipio)
      .tipo(request.tipoComunidade() != null ? request.tipoComunidade() : TipoComunidade.SITIO)
      .liderNome(request.liderNome())
      .liderTelefone(request.liderTelefone())
      .latitude(request.latitude())
      .longitude(request.longitude())
      .observacoes(request.observacoes())
      .build();

    return ComunidadeResponse.fromEntity(comunidadeRepositorio.save(comunidade));
  }
  public ComunidadeResponse atualizar(UUID id, ComunidadeUpdateRequest request) {
    Comunidade comunidade = comunidadeRepositorio.findById(id)
      .orElseThrow(() -> new ComunidadeNaoEncontradaException(id));

    Municipio municipio = buscarMunicipio(request.municipioId());

    comunidade.setNome(request.nome());
    comunidade.setMunicipio(municipio);
    comunidade.setTipo(request.tipoComunidade() != null ? request.tipoComunidade() : comunidade.getTipo());
    comunidade.setLiderNome(request.liderNome());
    comunidade.setLiderTelefone(request.liderTelefone());
    comunidade.setLatitude(request.latitude());
    comunidade.setLongitude(request.longitude());
    comunidade.setObservacoes(request.observacoes());

    // Entidade gerenciada pelo JPA (contexto de persistência aberto) -> o UPDATE
    // é disparado automaticamente no commit da transação, sem precisar de save().
    return ComunidadeResponse.fromEntity(comunidade);
  }



}
