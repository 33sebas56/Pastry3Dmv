package com.backend.pastry3d.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String from;
    private final String publicBaseUrl;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:no-reply@pastry3d.local}") String from,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl;
    }

    public void sendVerificationEmail(String to, String rawToken) {
        if (!mailEnabled) {
            return;
        }

        String link = publicBaseUrl + "/api/auth/confirm?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Confirma tu cuenta en Pastry3D");
        message.setText("Bienvenido a Pastry3D. Confirma tu cuenta entrando a este enlace: " + link);
        mailSender.send(message);
    }
}
