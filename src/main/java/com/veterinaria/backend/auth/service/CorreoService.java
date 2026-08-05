package com.veterinaria.backend.auth.service;

public interface CorreoService {

    void enviarCodigoRecuperacion(String correoDestino, String nombreCompleto, String codigo);
}
