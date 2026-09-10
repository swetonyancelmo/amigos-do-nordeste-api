package br.org.amigosdonordeste.cadastro.comum;

import br.org.amigosdonordeste.cadastro.auth.CredenciaisInvalidasException;
import br.org.amigosdonordeste.cadastro.auth.SenhaAtualIncorretaException;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.usuario.EmailJaCadastradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Toda resposta de erro sai no mesmo formato, com a chave `message`, porque e o
 * que o cliente do frontend le. Mensagem em portugues e util: quem vai ler e a
 * usuaria, nao o desenvolvedor.
 */
@RestControllerAdvice
public class ManipuladorDeErros {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> credenciais(CredenciaisInvalidasException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(SenhaAtualIncorretaException.class)
    public ResponseEntity<Map<String, Object>> senha(SenhaAtualIncorretaException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, Object>> emailDuplicado(EmailJaCadastradoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ComunidadeNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> comunidadeNaoEncontrada(ComunidadeNaoEncontradaException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MunicipioNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> municipioNaoEncontrado(MunicipioNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .findFirst()
            .orElse("Dados inválidos.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("em", OffsetDateTime.now().toString());
        corpo.put("status", status.value());
        corpo.put("message", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
