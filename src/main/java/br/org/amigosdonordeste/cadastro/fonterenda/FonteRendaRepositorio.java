package br.org.amigosdonordeste.cadastro.fonterenda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FonteRendaRepositorio extends JpaRepository<FonteRenda, UUID> {

    List<FonteRenda> findByFamiliaId(UUID familiaId);
}
