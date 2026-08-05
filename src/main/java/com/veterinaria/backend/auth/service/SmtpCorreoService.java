package com.veterinaria.backend.auth.service;

import com.veterinaria.backend.auth.exception.CorreoEnvioException;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmtpCorreoService implements CorreoService {

    private final JavaMailSender javaMailSender;
    private final AuthMailProperties authMailProperties;

    public SmtpCorreoService(JavaMailSender javaMailSender, AuthMailProperties authMailProperties) {
        this.javaMailSender = javaMailSender;
        this.authMailProperties = authMailProperties;
    }

    @Override
    public void enviarCodigoRecuperacion(String correoDestino, String nombreCompleto, String codigo) {
        if (!StringUtils.hasText(authMailProperties.getFrom())) {
            throw new CorreoEnvioException("La configuracion de correo no esta completa.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authMailProperties.getFrom());
        message.setTo(correoDestino);
        message.setSubject("Codigo de recuperacion - Veterinaria");
        message.setText(buildBody(nombreCompleto, codigo));

        try {
            javaMailSender.send(message);
        } catch (Exception exception) {
            throw new CorreoEnvioException("No fue posible enviar el correo de recuperacion.", exception);
        }
    }

    private String buildBody(String nombreCompleto, String codigo) {
        return """
                Hola %s,

                Recibimos una solicitud para recuperar el acceso al sistema de la veterinaria.

                Tu codigo de recuperacion es: %s
                Este codigo vence en %d minutos.

                No compartas este codigo con nadie. Si no realizaste esta solicitud, puedes ignorar este mensaje.
                """
                .formatted(nombreCompleto, codigo, authMailProperties.getRecoveryCodeExpirationMinutes());
    }
}
