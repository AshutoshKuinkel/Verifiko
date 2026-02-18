package com.verifico.server.email;

import com.verifico.server.user.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmailForv1(User user) {
        String html = loadHtmlContentFilePathForEmail(user,
                "emails/WelcomeEmail4V1.html")
                .replace("{{timestamp}}", LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd mmm, yyyy 'at' HH:mm")));

        sendHtmlEmail(user.getEmail(),
                "Congratulations for stepping into your new journey with Verifiko!", html);
    }

    @Async
    public void sendCreditPurchaseReceiptForv1(User user, int creditsAmount, double price) {
        String html = loadHtmlContentFilePathForEmail(user,
                "emails/CreditPurchaseDigitalReceipt4V1.html")
                .replace("{{credits}}", String.valueOf(creditsAmount))
                .replace("{{price}}", String.format("$%.2f", price))
                .replace("{{date}}", LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd mmm,yyyy")));

        sendHtmlEmail(user.getEmail(), "Verifiko purchase digital receipt", html);
    }

    @Async
    public void sendPasswordChangedEmailForv1(User user) {
        String html = loadHtmlContentFilePathForEmail(user,
                "emails/PasswordChangedNotification4V1.html")
                .replace("{{timestamp}}", LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd mmm, yyyy 'at' HH:mm")));

        sendHtmlEmail(user.getEmail(), "Your password was changed", html);
    }

    // helper template loader
    private String loadTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load email template: {}", path);
            return "";
        }
    }

    private String loadHtmlContentFilePathForEmail(User user, String path) {
        return loadTemplate(path).replace("{{username}}", user.getUsername());
    }
}
