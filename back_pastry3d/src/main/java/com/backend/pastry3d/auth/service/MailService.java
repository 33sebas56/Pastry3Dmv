package com.backend.pastry3d.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String from;
    private final String frontendBaseUrl;
    private final int verificationExpirationMinutes;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:no-reply@pastry3d.local}") String from,
            @Value("${app.frontend-base-url:${app.public-base-url:http://localhost:5173}}") String frontendBaseUrl,
            @Value("${app.mail.register-code-expiration-minutes:30}") int verificationExpirationMinutes
    ) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl;
        this.verificationExpirationMinutes = verificationExpirationMinutes;
    }

    public void sendVerificationEmail(String to, String rawToken) {
        if (!mailEnabled) {
            return;
        }

        String link = buildConfirmationLink(rawToken);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setReplyTo(from);
            helper.setSubject("Confirma tu cuenta en Pastry3D");
            helper.setText(buildPlainText(link), buildHtmlEmail(link));

            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("No se pudo construir el correo de verificación", exception);
        }
    }

    private String buildConfirmationLink(String rawToken) {
        String normalizedBaseUrl = normalizeBaseUrl(frontendBaseUrl);
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return normalizedBaseUrl + "/confirm-email?token=" + encodedToken;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:5173";
        }

        String normalized = value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String buildPlainText(String link) {
        return "Bienvenido a Pastry3D\n\n"
                + "Confirma tu cuenta para activar el acceso al laboratorio de recetas y escenas 3D.\n\n"
                + "Confirmar cuenta: " + link + "\n\n"
                + "Este enlace vence en " + verificationExpirationMinutes + " minutos. "
                + "Si no creaste esta cuenta, puedes ignorar este mensaje.";
    }

    private String buildHtmlEmail(String link) {
        String safeLink = htmlEscape(link);
        String expiration = String.valueOf(verificationExpirationMinutes);

        return "<!doctype html>"
                + "<html lang=\"es\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Confirma tu cuenta en Pastry3D</title>"
                + "</head>"
                + "<body style=\"margin:0;padding:0;background:#fff8f2;color:#211816;font-family:Inter,Segoe UI,Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:linear-gradient(135deg,#fff8f2 0%,#fff1e7 100%);padding:32px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:640px;background:#ffffff;border:1px solid #f0d7c7;border-radius:28px;overflow:hidden;box-shadow:0 24px 70px rgba(111,62,35,0.14);\">"
                + "<tr>"
                + "<td style=\"padding:34px 34px 18px;background:radial-gradient(circle at top left,rgba(255,154,95,0.25),transparent 260px),#ffffff;\">"
                + "<div style=\"width:56px;height:56px;border-radius:18px;background:linear-gradient(135deg,#ff9a5f,#d85f2c);box-shadow:0 16px 36px rgba(216,95,44,0.28);display:inline-block;text-align:center;line-height:56px;color:#ffffff;font-size:26px;font-weight:800;\">P3D</div>"
                + "<p style=\"margin:22px 0 6px;color:#d85f2c;text-transform:uppercase;letter-spacing:0.12em;font-size:12px;font-weight:800;\">Pastry3D</p>"
                + "<h1 style=\"margin:0;color:#211816;font-size:32px;line-height:1.08;letter-spacing:-0.03em;\">Confirma tu cuenta</h1>"
                + "<p style=\"margin:14px 0 0;color:#7e6860;font-size:16px;line-height:1.65;\">Activa tu acceso al laboratorio de recetas con IA y composiciones 3D. Solo necesitamos verificar que este correo te pertenece.</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding:18px 34px 10px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#fff8f2;border:1px solid #f0d7c7;border-radius:22px;\">"
                + "<tr><td style=\"padding:22px;\">"
                + "<p style=\"margin:0 0 18px;color:#4d3b35;font-size:15px;line-height:1.6;\">Haz clic en el botón para confirmar la cuenta. El enlace vence en <strong>" + expiration + " minutos</strong>.</p>"
                + "<a href=\"" + safeLink + "\" style=\"display:inline-block;background:linear-gradient(135deg,#d85f2c,#ff9a5f);color:#ffffff;text-decoration:none;border-radius:16px;padding:14px 22px;font-weight:800;box-shadow:0 14px 30px rgba(216,95,44,0.25);\">Confirmar cuenta</a>"
                + "</td></tr>"
                + "</table>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding:14px 34px 34px;\">"
                + "<p style=\"margin:0 0 10px;color:#7e6860;font-size:13px;line-height:1.55;\">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>"
                + "<p style=\"margin:0;color:#a9431c;font-size:13px;line-height:1.55;word-break:break-all;\"><a href=\"" + safeLink + "\" style=\"color:#a9431c;text-decoration:underline;\">" + safeLink + "</a></p>"
                + "<hr style=\"border:none;border-top:1px solid #f0d7c7;margin:24px 0;\">"
                + "<p style=\"margin:0;color:#7e6860;font-size:12px;line-height:1.5;\">Si no creaste esta cuenta, puedes ignorar este mensaje. No se activará ningún acceso sin la confirmación.</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
