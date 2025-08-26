package project1.ares.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import project1.ares.templates.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    private JavaMailSender mailSender;

    private final EmailTemplates emailTemplates = new EmailTemplates();

    public Mono<Boolean> sendEmail(String to, String subject, int template_num, Map<String, String> params) {
        System.out.println("Procesando email");
        try {
            System.out.println("Email enviado");
            String body = emailTemplates.buildTemplate(template_num, params);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            return Mono.just(true);
        } catch (MessagingException e) {
            log.error("Email não enviado:" + e.getMessage());
            return Mono.just(false);
        }
    }
}
