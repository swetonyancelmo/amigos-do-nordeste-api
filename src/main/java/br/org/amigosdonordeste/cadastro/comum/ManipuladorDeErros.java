package br.org.amigosdonordeste.cadastro.comum;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.org.amigosdonordeste.cadastro.agente.CodigoConviteInvalidoException;
import br.org.amigosdonordeste.cadastro.auth.CredenciaisInvalidasException;
import br.org.amigosdonordeste.cadastro.auth.SenhaAtualIncorretaException;
import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.FamiliaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdDuplicadoNoPayloadException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdadeEstimadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.exception.NumeroCalcadoInvalidoException;
import br.org.amigosdonordeste.cadastro.familia.exception.PessoaReferenciadaInvalidaException;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroInvalidoException;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroJaAvaliadoException;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.usuario.EmailJaCadastradoException;

/**
 * Toda resposta de erro sai no mesmo formato, com a chave `message`, porque e o
 * que o cliente do frontend le. Mensagem em portugues e util: quem vai ler e a
 * usuaria, nao o desenvolvedor.
 */
@RestControllerAdvice
public class ManipuladorDeErros {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> credenciais(CredenciaisInvalidasException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(CodigoConviteInvalidoException.class)
    public ResponseEntity<ErroResposta> codigoConvite(CodigoConviteInvalidoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroResposta> muitasTentativas(MuitasTentativasException e) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(SenhaAtualIncorretaException.class)
    public ResponseEntity<ErroResposta> senha(SenhaAtualIncorretaException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResposta> emailDuplicado(EmailJaCadastradoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ComunidadeNaoEncontradaException.class)
    public ResponseEntity<ErroResposta> comunidadeNaoEncontrada(ComunidadeNaoEncontradaException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MunicipioNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> municipioNaoEncontrado(MunicipioNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(FamiliaNaoEncontradaException.class)
    public ResponseEntity<ErroResposta> familiaNaoEncontrada(FamiliaNaoEncontradaException e) {
    return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PessoaReferenciadaInvalidaException.class)
    public ResponseEntity<ErroResposta> pessoaReferenciadaInvalida(PessoaReferenciadaInvalidaException e) {
    return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NumeroCalcadoInvalidoException.class)
    public ResponseEntity<ErroResposta> numeroCalcadoInvalido(NumeroCalcadoInvalidoException e) {
    return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IdadeEstimadaInvalidaException.class)
    public ResponseEntity<ErroResposta> idadeEstimadaInvalida(IdadeEstimadaInvalidaException e) {
    return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IdDuplicadoNoPayloadException.class)
    public ResponseEntity<ErroResposta> idDuplicado(IdDuplicadoNoPayloadException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PreCadastroInvalidoException.class)
    public ResponseEntity<ErroResposta> preCadastroInvalido(PreCadastroInvalidoException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PreCadastroNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> preCadastroNaoEncontrado(PreCadastroNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PreCadastroJaAvaliadoException.class)
    public ResponseEntity<ErroResposta> preCadastroJaAvaliado(PreCadastroJaAvaliadoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Rede de segurança: constraint do banco (tamanho, check, unique) que
     * escapou do Bean Validation. Sem isso viraria 500 com stack trace.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos: violam uma restrição do cadastro.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .findFirst()
            .orElse("Dados inválidos.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem) {
        ErroResposta corpo = new ErroResposta(OffsetDateTime.now().toString(), status.value(), mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
