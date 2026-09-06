package br.org.amigosdonordeste.cadastro.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PessoaRepositorio extends JpaRepository<Pessoa, UUID> {

    List<Pessoa> findByFamiliaId(UUID familiaId);

    /** RF-09: cadastros marcados para completar depois. */
    List<Pessoa> findByCadastroIncompletoTrue();
}
