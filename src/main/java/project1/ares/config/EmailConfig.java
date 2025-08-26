package project1.ares.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import project1.ares.model.Mail;
import reactor.core.publisher.Mono;

import java.util.Properties;

@Configuration
public class EmailConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    public EmailConfig(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Bean
    public Mono<JavaMailSender> mailSender() {
;
        return mongoTemplate.findById("system_mails", Mail.class)
                .map(mail -> {
                    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
                    mailSender.setHost("smtp.gmail.com");
                    mailSender.setPort(587);
                    mailSender.setUsername(mail.getMail());
                    mailSender.setPassword(mail.getPass());

                    Properties props = mailSender.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.debug", "true");

                    mailSender.setJavaMailProperties(props);
                    return mailSender;
                });
    }
}
