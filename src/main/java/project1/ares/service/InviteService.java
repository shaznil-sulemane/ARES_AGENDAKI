package project1.ares.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import project1.ares.config.ApiResponse;
import project1.ares.model.Company;
import project1.ares.model.Invite;
import project1.ares.model.User;
import project1.ares.repository.CompanyRepository;
import project1.ares.repository.UserRepository;
import project1.ares.templates.MessageTemplates;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InviteService {

    private static final Logger log = LoggerFactory.getLogger(InviteService.class);
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final CompanyRepository companyRepository;

    public InviteService(UserRepository userRepository, EmailService emailService, NotificationService notificationService, SequenceGeneratorService sequenceGeneratorService, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.companyRepository = companyRepository;
    }

    public Mono<ResponseEntity<ApiResponse<String>>> invite(String companyId, String identifier, List<Invite.Channel> channels) {
        Mono<User> userMono = userRepository.findByIdentifier(identifier);
        Mono<Company> companyMono = companyRepository.findById(companyId);

        log.info("Invite 1");
        return Mono.zip(userMono, companyMono)
                .flatMap(tuple -> {
                    log.info("Invite 2");
                    User user = tuple.getT1();
                    Company company = tuple.getT2();

                    return sequenceGeneratorService.generateId("invites", "INV", 10)
                            .flatMap(inv -> {
                                log.info("Invite 3");

                    Invite newInvite = new Invite();
                    newInvite.setIdentifier(identifier);
                    newInvite.setChannels(channels);
                    newInvite.setCompanyId(companyId);
                    newInvite.setExpitesAt(LocalDateTime.now().plusDays(1));

                    if(channels.contains(Invite.Channel.WHATSAPP)) {
                        log.info("Invite channel WHATSAPP");
                        log.info(user.getPhoneNumber());
                        Map<String, String> body = Map.of(
                                "companyName", company.getName(),
                                "name", user.getFullName(),
                                "link", "https://medx.krypthon.com",
                                "phone_number", user.getPhoneNumber()
                        );
                        String message = MessageTemplates.buildTemplate(1, body);
                        notificationService.sendWhatsappNotification("258" + user.getPhoneNumber(), message).subscribe(
                                aBoolean -> {
                                    log.info("Message sent successfully");
                                },
                                throwable -> {
                                    log.error("Error sending message");
                                }
                        );
                    }

                    return Mono.just(ResponseEntity.ok(new ApiResponse<>()));
                            });
                });
    }

}
