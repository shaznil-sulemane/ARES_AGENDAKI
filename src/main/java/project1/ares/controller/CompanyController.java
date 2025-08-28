package project1.ares.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import project1.ares.config.ApiResponse;
import project1.ares.config.CustomUserDetails;
import project1.ares.config.FileStorageConfig;
import project1.ares.config.GlobalExceptionHandler;
import project1.ares.dto.create.CompanyCREATE;
import project1.ares.model.*;
import project1.ares.repository.CompanyRepository;
import project1.ares.repository.PlanRepository;
import project1.ares.repository.UserRepository;
import project1.ares.service.ImageService;
import project1.ares.service.SequenceGeneratorService;
import project1.ares.util.Regex;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);
    private final CompanyRepository companyRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final FileStorageConfig fileConfig;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final WebClient.Builder webClientBuilder;
    private final PlanRepository planRepository;

    public CompanyController(CompanyRepository companyRepository, SequenceGeneratorService sequenceGeneratorService, FileStorageConfig fileConfig, UserRepository userRepository, ImageService imageService, WebClient.Builder webClientBuilder, PlanRepository planRepository) {
        this.companyRepository = companyRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.fileConfig = fileConfig;
        this.userRepository = userRepository;
        this.imageService = imageService;
        this.webClientBuilder = webClientBuilder;
        this.planRepository = planRepository;
    }

    @GetMapping
    public Flux<Company> getAll() {
        return companyRepository.findAll();
    }

    @GetMapping("/{ownerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and #ownerId == #principal.id)")
    public Mono<ResponseEntity<ApiResponse<List<Map<String, Object>>>>> getCompaniesByOwner(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String ownerId) {

        return companyRepository.findCompaniesByOwner(ownerId)
                .map(company -> Map.of(
                        "companyId", (Object) company.getId(),
                        "companyName", (Object) company.getName()
                ))
                .collectList()
                .map(companies -> {
                    if (companies.isEmpty()) {
                        return ResponseEntity.status(404).body(
                                new ApiResponse<List<Map<String, Object>>>(
                                        false,
                                        "Nenhuma empresa encontrada",
                                        null
                                )
                        );
                    }
                    return ResponseEntity.ok(
                            new ApiResponse<>(
                                    true,
                                    "Empresas encontradas com sucesso",
                                    companies
                            )
                    );
                });
    }


//    @GetMapping("/{id}")
//    public Mono<ResponseEntity<Company>> getById(@PathVariable String id) {
//        return companyRepository.findById(id)
//                .map(ResponseEntity::ok)
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    @PostMapping
    @PreAuthorize("(hasRole('MANAGER') and #principal.id == #ownerId) or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Company>>> createCompany(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("owner") String ownerId,
            @RequestPart("name") String name,
            @RequestPart("email") String email,
            @RequestPart("phone") String phone,
            @RequestPart("address") String address,
            @RequestPart("logo") Mono<FilePart> logo,
            @RequestPart("banner") Mono<FilePart> banner
    ) {

        if(!principal.getId().equals(ownerId) && principal.getRole() != User.Role.ADMIN) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Acesso negado.", null)));

        }
        // 🔹 Validações iniciais
        if (address == null || address.isBlank()) {
            return badRequest("Endereço inválido.");
        }
        if (!Regex.isValidPhone(phone)) {
            return badRequest("Telefone inválido.");
        }
        if (!Regex.isValidEmail(email)) {
            return badRequest("Email inválido.");
        }
        if (!Regex.isValidFullName(name)) {
            return badRequest("Nome da empresa inválido.");
        }
        if (logo == null) {
            return badRequest("Logótipo é obrigatório.");
        }
        if (banner == null) {
            return badRequest("Banner é obrigatório.");
        }

        // 🔹 Valores padrão
        CompanyType finalType = CompanyType.SALON;
        GeoLocation finalLocation = new GeoLocation(0.0, 0.0);
        Map<String, Object> finalMetadata = new HashMap<>();


        return sequenceGeneratorService.generateCompanyId()
                .flatMap(compId -> {
                    // 🔹 Verificação do dono + upload
                    return Mono.zip(userRepository.existsById(ownerId), userRepository.findById(ownerId), logo, banner)
                            .flatMap(tuple -> {
                                boolean ownerExists = tuple.getT1();
                                User owner = tuple.getT2();
                                FilePart logoFile = tuple.getT3();
                                FilePart bannerFile = tuple.getT4();

                                if (!ownerExists || owner == null) {
                                    return badRequest("Usuário não existe.");
                                }

                                Company newCompany = new Company();
                                newCompany.setId(compId);
                                newCompany.setName(name);
                                newCompany.setEmail(email);
                                newCompany.setPhone(phone);
                                newCompany.setAddress(address);
                                newCompany.setMetadata(finalMetadata);
                                newCompany.setType(finalType);
                                newCompany.setActive(false);
                                newCompany.setPlan(null);
                                newCompany.setLocation(finalLocation);
                                newCompany.setOwner(owner.getId());
                                newCompany.setOwnerName(owner.getFullName());
                                newCompany.setPlanEndDate(LocalDate.now());
                                newCompany.setPlanStartDate(LocalDate.now());

                                return companyRepository.save(newCompany)
                                        .flatMap(savedCompany -> processImages(savedCompany, logoFile, bannerFile));
                            });
                });
    }

    // 🔹 Processa logo + banner
    private Mono<ResponseEntity<ApiResponse<Company>>> processImages(Company company, FilePart logoFile, FilePart bannerFile) {
        String basePath = fileConfig.getCompany(company.getId());
        File dir = new File(basePath);
        if (!dir.exists() && !dir.mkdirs()) {
            return internalError("Não foi possível criar diretório para salvar imagens.");
        }

        String logoPath = basePath + "/logo.png";
        String bannerPath = basePath + "/banner.png";

        return Mono.zip(
                imageService.validateAndProcessImage(logoFile),
                imageService.validateAndProcessImage(bannerFile)
        ).flatMap(images -> {
            try {
                Files.write(Path.of(logoPath), images.getT1());
                Files.write(Path.of(bannerPath), images.getT2());
                return success("Empresa salva com sucesso.", company);
            } catch (IOException e) {
                return internalError("Erro ao salvar imagens.");
            }
        });
    }

    // 🔹 Helpers para respostas
    private Mono<ResponseEntity<ApiResponse<Company>>> badRequest(String message) {
        return Mono.just(ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null)));
    }

    private Mono<ResponseEntity<ApiResponse<Company>>> internalError(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, message, null)));
    }

    private Mono<ResponseEntity<ApiResponse<Company>>> success(String message, Company data) {
        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, message, data)));
    }




//    @PostMapping
//    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
//    public Mono<ResponseEntity<ApiResponse<?>>> create(
//            @AuthenticationPrincipal CustomUserDetails principal,
//            @RequestBody CompanyCREATE req
//    ) {
//        Company company = new Company();
//        company.setOwner(principal.getId());
//        company.setName(req.getName());
//        company.setEmail(req.getEmail());
//        company.setPhone(req.getPhone());
//        company.setAddress(req.getAddress());
//        company.setType(req.getType());
//        //company.setType(CompanyType.valueOf(((String) body.getOrDefault("type", "OTHER")).toUpperCase()));
//        company.setActive(true);
//
//        return sequenceGeneratorService.generateCompanyId()
//                .flatMap(compId -> {
//                    log.info("Creating new company {}", compId);
//                    company.setId(compId);
//                    return companyRepository.save(company)
//                            .map(savedCompany -> {
//                                return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(false, "Empresa criada com sucesso.", savedCompany));
//                            });
//                });
//    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<Company>>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String id,
            @RequestBody CompanyCREATE req) {

        String name = req.getName();
        String email = req.getEmail();
        String phone = req.getPhone();

        if (!Regex.isValidEmail(email)) {
            return Mono.error(new IllegalArgumentException("Email inválido."));
        }
        if (!Regex.isValidPhone(phone)) {
            return Mono.error(new IllegalArgumentException("Telefone inválido."));
        }
        if (!Regex.isValidFullName(name)) {
            return Mono.error(new IllegalArgumentException("Nome do salão inválido."));
        }

        return companyRepository.findById(id)
                .flatMap(existing -> {
                    if (!existing.getOwner().equals(principal.getId())) {
                        return Mono.error(new GlobalExceptionHandler.UnauthorizedException("Access denied."));
                    }
                    existing.setName(req.getName());
                    existing.setEmail(req.getEmail());
                    existing.setPhone(req.getPhone());
                    existing.setAddress(req.getAddress());
                    existing.setLocation(req.getLocation());
                    existing.setMetadata(req.getMetadata());
                    existing.setType(req.getType());
                    existing.setUpdatedAt(java.time.Instant.now());

                    return companyRepository.save(existing)
                            .map(_company ->
                                    ResponseEntity.ok(
                                            new ApiResponse<>(true, "Empresa atualizada com sucesso.", _company)
                                    )
                            );
                })
                .defaultIfEmpty(
                        ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ApiResponse<>(false, "Empresa não encontrada.", null))
                );
    }


    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return companyRepository.findById(id)
                .flatMap(existing ->
                        companyRepository.delete(existing)
                                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

//    private static final String API_URL = "https://paysuite.tech/api/v1";
    private static final String API_URL = "http://localhost:3000/api/v1";

    private static final String AUTH_TOKEN = "490|Y97U2s8OLOMtywgnwlRrX0FS9P6KH1x2BFQa2pFabe693b8d";


    @PostMapping("/pay")
    @PreAuthorize("hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<String>>> pay(@RequestBody Map<String, String> bodyReq) {
        String planId = bodyReq.get("planId");
        String companyId = bodyReq.get("companyId");
        String description = (String) bodyReq.getOrDefault("description", "Pedido padrão");

        // Se reference não fornecida → gerar sequencial
        Mono<String> referenceMono;
        if (bodyReq.containsKey("reference")) {
            referenceMono = Mono.just((String) bodyReq.get("reference"));
        } else {
            referenceMono = sequenceGeneratorService.generateId("invoice", "INV", 7);
        }

        return planRepository.findById(planId)
                .flatMap(plan -> {
                    return companyRepository.findById(companyId)
                            .flatMap(company -> {
                                if(company.getPlan().getUrlToPay() != null) {
                                    return Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Salva com sucesso.", company.getPlan().getUrlToPay())));
                                }
                                if(plan.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                                                Company.Plan newPlan = new Company.Plan();
                                                newPlan.setName(plan.getName());
                                                newPlan.setDescription(description);
                                                newPlan.setBadge(plan.getBadge());
                                                newPlan.setPrice(plan.getPrice());
                                                newPlan.setActive(true);
                                                newPlan.setFeatures(plan.getFeatures());
                                                newPlan.setDuration(plan.getDuration());
                                                newPlan.setTitle(plan.getTitle());
                                                newPlan.setPosition(plan.getPosition());

                                                company.setPlanStartDate(LocalDate.now());
                                                company.setPlanEndDate(LocalDate.now().plusMonths(1));

                                                company.setPlan(newPlan);
                                                companyRepository.save(company).subscribe();
                                                return Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Salva com sucesso.", null)));
                                }
                                return referenceMono.flatMap(reference -> {
                                    Map<String, Object> body = Map.of(
                                            "amount", plan.getPrice().toString(),
                                            "reference", reference,
                                            "description", description,
                                            "return_url", "http://192.168.0.103:5173/dashboard/manager",
                                            "callback_url", "https://bbea15be054b.ngrok-free.app/companies/paysuite/" + companyId
                                    );

                                    return webClientBuilder.baseUrl(API_URL).build()
                                            .post()
                                            .uri("/payments")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header("Authorization", "Bearer " + AUTH_TOKEN)
                                            .body(BodyInserters.fromValue(body))
                                            .retrieve()
                                            .bodyToMono(Map.class)
                                            .flatMap(response -> {
                                                Map<String, Object> data = (Map<String, Object>) response.get("data");
                                                String checkoutUrl = (String) data.get("checkout_url"); // pega o checkout_url// pega o id do pagamento
                                                Company.Plan newPlan = new Company.Plan();
                                                newPlan.setName(plan.getName());
                                                newPlan.setDescription(description);
                                                newPlan.setBadge(plan.getBadge());
                                                newPlan.setPrice(plan.getPrice());
                                                newPlan.setActive(false);
                                                newPlan.setFeatures(plan.getFeatures());
                                                newPlan.setDuration(plan.getDuration());
                                                newPlan.setTitle(plan.getTitle());
                                                newPlan.setPosition(plan.getPosition());
                                                newPlan.setUrlToPay(checkoutUrl);
                                                company.setPlanStartDate(LocalDate.now());
                                                company.setPlanEndDate(LocalDate.now().plusMonths(1));
                                                company.setPlan(newPlan);
                                                companyRepository.save(company).subscribe();
                                                return Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Plano gerado.", checkoutUrl)));
                                            });
                        });
                    });
                });
    }

    @PostMapping("/paysuite/{companyId}")
    public Mono<ResponseEntity<Void>> webhook(@RequestBody Map<String, Object> body, @PathVariable String companyId) {
        System.out.println("Recebido webhook: " + body);

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String paymentId = (String) data.get("id");

        return webClientBuilder.baseUrl(API_URL).build()
                .get()
                .uri("/payments/{id}", paymentId != null ? paymentId : "REQ6xcz")
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap((payment -> {
                    System.out.println("Pagamento atualizado: " + payment);
                    Map<String, Object> _data = (Map<String, Object>) payment.get("data");
                    Map<String, Object> transaction = (Map<String, Object>) _data.get("transaction");
                    String status = (String) transaction.get("status");

                    if ("completed".equalsIgnoreCase(status)) {
                        return companyRepository.findById(companyId)
                                .flatMap(company -> {

                                    company.setPlanStartDate(LocalDate.now());
                                    company.setPlanEndDate(LocalDate.now().plusMonths(1));
                                    company.setActive(true);
                                    company.getPlan().setUrlToPay(null);
                                    companyRepository.save(company).subscribe();
                                    return Mono.just(ResponseEntity.ok().build());
                                });
                    }
                    return Mono.just(ResponseEntity.ok().build());
                }));
    }
}
