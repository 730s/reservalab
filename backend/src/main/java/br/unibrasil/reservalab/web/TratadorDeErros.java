package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.ApiException;
import br.unibrasil.reservalab.web.Dtos.ErroRes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErroRes> negocio(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErroRes(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroRes> validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).findFirst().orElse("Dados inválidos.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroRes("E01", msg));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErroRes> requisicao(Exception e) {
        return ResponseEntity.badRequest().body(new ErroRes("E01", "Requisição inválida: verifique datas, horários e campos informados."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroRes> grande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErroRes("E01", "Arquivo acima de 5 MB."));
    }
}
