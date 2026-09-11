package com.veterinaria.backend.pago.exception;
public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException(Long id) { super("No se encontro el pago " + id + "."); }
}
