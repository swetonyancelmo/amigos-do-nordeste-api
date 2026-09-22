package br.org.amigosdonordeste.cadastro.municipio;

import br.org.amigosdonordeste.cadastro.comum.RecursoNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.municipio.dto.MunicipioRequisicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MunicipioService {

    private final MunicipioRepositorio municipios;

    // O Spring passa o repositório sozinho aqui. Você nunca dá `new` em nada.
    public MunicipioService(MunicipioRepositorio municipios) {
        this.municipios = municipios;
    }

    @Transactional(readOnly = true)
    public List<Municipio> listar() {
        return municipios.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Municipio criar(MunicipioRequisicao dados) {
        String codigo = normalizar(dados.codigoIbge());

        if (codigo != null && municipios.existsByCodigoIbge(codigo)) {
            throw new CodigoIbgeJaCadastradoException();
        }

        Municipio m = new Municipio();
        aplicar(m, dados, codigo);
        return municipios.save(m);
    }

    @Transactional
    public Municipio atualizar(UUID id, MunicipioRequisicao dados) {
        Municipio m = municipios.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Município não encontrado."));

        String codigo = normalizar(dados.codigoIbge());

        // ...AndIdNot: o município pode manter o próprio código ao ser editado.
        // Sem isso, salvar sem mudar nada daria 409 — o bug clássico dessa rota.
        if (codigo != null && municipios.existsByCodigoIbgeAndIdNot(codigo, id)) {
            throw new CodigoIbgeJaCadastradoException();
        }

        aplicar(m, dados, codigo);
        return m;   // dentro de @Transactional, o JPA salva sozinho ao terminar o método
    }

    private void aplicar(Municipio m, MunicipioRequisicao dados, String codigo) {
        m.setNome(dados.nome().trim());
        m.setUf(dados.uf().toUpperCase());
        m.setCodigoIbge(codigo);
    }

    /** Campo vazio vindo do formulário vira null, não string vazia. */
    private String normalizar(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return codigo.trim();
    }
}
