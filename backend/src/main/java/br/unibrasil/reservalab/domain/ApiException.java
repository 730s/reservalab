package br.unibrasil.reservalab.domain;

/** Erro de negócio com código (E01..E06 do Quadro 13; E00 para demais regras). */
public class ApiException extends RuntimeException {
    private final String codigo;
    private final int status;

    public ApiException(String codigo, int status, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }
    public String getCodigo() { return codigo; }
    public int getStatus() { return status; }

    public static ApiException e(String codigo, int status, String msg) { return new ApiException(codigo, status, msg); }
    public static ApiException proibido(String msg) { return new ApiException("E00", 403, msg); }
    public static ApiException naoEncontrado(String msg) { return new ApiException("E00", 404, msg); }
    public static ApiException regra(String msg) { return new ApiException("E00", 409, msg); }
    public static ApiException invalido(String msg) { return new ApiException("E01", 422, msg); }
}
