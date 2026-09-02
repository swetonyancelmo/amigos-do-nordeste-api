package br.org.amigosdonordeste.cadastro.usuario;

import br.org.amigosdonordeste.cadastro.usuario.dto.CriarUsuarioRequisicao;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioRepositorio usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Transactional
    public Usuario criar(CriarUsuarioRequisicao dados) {
        String email = dados.email().toLowerCase().trim();
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailJaCadastradoException();
        }
        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(codificador.encode(dados.senha()));
        usuario.setAtivo(true);
        return usuarios.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAll();
    }
}
