package br.org.amigosdonordeste.cadastro.usuario;

import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.usuario.dto.CriarUsuarioRequisicao;
import br.org.amigosdonordeste.cadastro.usuario.dto.UsuarioResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cadastro de usuário — e a decisão por trás dele merece uma linha.
 *
 * A dona da associação disse que uma pessoa cadastra e uma pessoa acessa. Por
 * isso este endpoint NAO é público: só quem já está autenticado pode criar
 * outra conta. Um registro aberto num sistema com uma usuária seria um buraco
 * de segurança sem nenhum benefício.
 *
 * A primeira conta, que não tem ninguém autenticado para criá-la, nasce do
 * perfil `criar-usuario` (ver CriarUsuarioRunner).
 *
 * Se a questão Q-01 de docs/requisitos.md for respondida com "mais de uma
 * pessoa usa o sistema", este endpoint já é o lugar certo — o que faltará é
 * papel/permissão na tabela `usuario`, não uma rota nova.
 */
@Tag(name = "usuarios")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Cria uma conta de acesso (exige estar autenticado)")
    @SecurityRequirement(name = "bearer")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UsuarioResposta criar(@Valid @RequestBody CriarUsuarioRequisicao dados) {
        return UsuarioResposta.de(service.criar(dados));
    }

    @Operation(summary = "Lista as contas existentes")
    @SecurityRequirement(name = "bearer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping
    public List<UsuarioResposta> listar() {
        return service.listar().stream().map(UsuarioResposta::de).toList();
    }
}
