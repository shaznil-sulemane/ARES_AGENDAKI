package project1.ares.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project1.ares.config.ApiResponse;
import project1.ares.config.CustomUserDetails;
import project1.ares.config.FileStorageConfig;
import project1.ares.config.GlobalExceptionHandler;
import project1.ares.dto.create.CompanyCREATE;
import project1.ares.model.*;
import project1.ares.repository.CompanyRepository;
import project1.ares.repository.ServiceRepository;
import project1.ares.service.ImageService;
import project1.ares.service.SequenceGeneratorService;
import project1.ares.util.Regex;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/service")
public class ServiceController {

    private static final Logger log = LoggerFactory.getLogger(ServiceController.class);
    private final CompanyRepository companyRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final FileStorageConfig fileConfig;
    private final ImageService imageService;
    private final ServiceRepository serviceRepository;

    public ServiceController(CompanyRepository companyRepository, SequenceGeneratorService sequenceGeneratorService, FileStorageConfig fileConfig, ImageService imageService, ServiceRepository serviceRepository) {
        this.companyRepository = companyRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.fileConfig = fileConfig;
        this.imageService = imageService;
        this.serviceRepository = serviceRepository;
    }

//    @GetMapping
//    public Flux<Service> getAll() {
//        return serviceRepository.findAll();
//    }
//
//    @GetMapping("/{companyId}")
//    public Mono<ResponseEntity<ApiResponse<List<Service>>>> getServicesByCompanies(
//            @PathVariable String companyId) {
//
//        return serviceRepository.findByCompanyId(companyId)
//                .map(company -> company)
//                .collectList()
//                .map(services -> {
//                    if (services.isEmpty()) {
//                        return ResponseEntity.status(404).body(
//                                new ApiResponse<>(
//                                        false,
//                                        "Nenhum serviço encontrado.",
//                                        null
//                                )
//                        );
//                    }
//                    return ResponseEntity.ok(
//                            new ApiResponse<>(
//                                    true,
//                                    "Serviços encontrados com sucesso",
//                                    services
//                            )
//                    );
//                });
//    }

    // 🔹 Atualizar serviço
    @PutMapping("/{serviceId}")
    @PreAuthorize("(hasRole('MANAGER') and #principal.id != null) or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Service>>> updateService(
            @PathVariable("serviceId") String serviceId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("name") String name,
            @RequestPart("description") String description,
            @RequestPart("price") String _price,
            @RequestPart("category") String category,
            @RequestPart("durationMinutes") String _durationMinutes,
            @RequestPart("active") String _active,
            @RequestPart(name = "banner", required = false) Mono<FilePart> banner
    ) {

        double price = Double.parseDouble(_price);
        int durationMinutes = Integer.parseInt(_durationMinutes);
        boolean active = Boolean.parseBoolean(_active);
        log.info(serviceId);

        serviceRepository.findById("SV00000012").subscribe(s -> log.info("Service: " + s.getId()));


        return serviceRepository.findById(serviceId)
                .flatMap(service -> companyRepository.findById(service.getCompanyId())
                        .flatMap(company -> {
                            boolean isOwner = company.getOwner().equals(principal.getId());
                            boolean isAdmin = principal.getRole().equals(User.Role.ADMIN);
                            if (!isOwner && !isAdmin) {
                                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(new ApiResponse<Service>(false, "Acesso negado.", null)));
                            }

                            // Atualiza campos
                            service.setName(name);
                            service.setDescription(description);
                            service.setPrice(BigDecimal.valueOf(price));
                            service.setCategory(category);
                            service.setDurationMinutes(durationMinutes);
                            service.setActive(active);

                            Mono<Service> updated = serviceRepository.save(service);

                            return updated.flatMap(saved ->
                                    banner.flatMap(filePart -> processBanner(saved, Mono.just(filePart), "Serviço atualizado com sucesso."))
                                            .switchIfEmpty(success("Serviço atualizado com sucesso.", saved))
                            );

                        }))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().body(new ApiResponse<>(false, "Serviço não encontrado.", null))));
    }

    // 🔹 Deletar serviço
    @DeleteMapping("/{serviceId}")
    @PreAuthorize("(hasRole('MANAGER') and #principal.id != null) or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteService(
            @PathVariable("serviceId") String serviceId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return serviceRepository.findById(serviceId)
                .flatMap(service -> companyRepository.findById(service.getCompanyId())
                        .flatMap(company -> {
                            boolean isOwner = company.getOwner().equals(principal.getId());
                            boolean isAdmin = principal.getRole().equals(User.Role.ADMIN);

                            if (!isOwner && !isAdmin) {
                                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(new ApiResponse<Void>(false, "Acesso negado.", null)));
                            }

                            return serviceRepository.deleteById(serviceId)
                                    .then(imageService.deleteCompanyFolder(fileConfig.getService(serviceId)))
                                    .thenReturn(ResponseEntity.ok(new ApiResponse<Void>(true, "Serviço eliminado com sucesso.", null)));
                        }))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().body(new ApiResponse<>(false, "Serviço não encontrado.", null))));
    }

    // 🔹 Listar todos os serviços
    @GetMapping
    public Flux<Service> listAllServices() {
        return serviceRepository.findAll();
    }

    // 🔹 Listar serviços do usuário logado (MANAGER → das suas empresas, ADMIN → todos)
    @GetMapping("/{companyId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public Flux<Service> listMyServices(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String companyId) {
        if (principal.getRole().equals(User.Role.ADMIN)) {
            return serviceRepository.findByCompanyId(companyId);
        }

        return companyRepository.findCompaniesByOwner(principal.getId())
                .filter(company -> company.getId().equals(companyId))
                .flatMap(company -> serviceRepository.findByCompanyId(companyId));
    }

    @PostMapping
    @PreAuthorize("(hasRole('MANAGER') and #principal.id != null) or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Service>>> createService(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("companyId") String companyId,
            @RequestPart("name") String name,
            @RequestPart("description") String description,
            @RequestPart("price") String _price,
            @RequestPart("category") String category,
            @RequestPart("durationMinutes") String _durationMinutes,
            @RequestPart("active") String _active,
            @RequestPart("banner") Mono<FilePart> banner
    ) {


        double price = Double.parseDouble(_price);
        int durationMinutes = Integer.parseInt(_durationMinutes);
        boolean active = Boolean.parseBoolean(_active);

        // 🔹 Validações iniciais
        if (name == null || name.isBlank()) {
            return badRequest("Nome do serviço inválido.");
        }
        if (description == null || description.isBlank()) {
            return badRequest("Descrição inválida.");
        }
        if (price <= 0) {
            return badRequest("Preço deve ser maior que 0.");
        }
        if (category == null || category.isBlank()) {
            return badRequest("Categoria inválida.");
        }
        if (durationMinutes <= 0) {
            return badRequest("Duração inválida.");
        }
        if (banner == null) {
            return badRequest("Banner do serviço é obrigatório.");
        }

        // 🔹 Verifica empresa e permissões
        return companyRepository.findById(companyId)
                .flatMap(company -> {
                    boolean isOwner = company.getOwner().equals(principal.getId());
                    boolean isAdmin = principal.getRole().equals(User.Role.ADMIN);

                    if (!isOwner && !isAdmin) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ApiResponse<Service>(false, "Acesso negado.", null)));
                    }

                    Service service = new Service();
                    service.setCompanyId(companyId);
                    service.setName(name);
                    service.setDescription(description);
                    service.setPrice(BigDecimal.valueOf(price));
                    service.setCategory(category);
                    service.setDurationMinutes(durationMinutes);
                    service.setActive(active);

                    return sequenceGeneratorService.generateId("service", "SV", 8)
                            .flatMap(s -> {
                                service.setId(s);
                                return serviceRepository.save(service)
                                        .flatMap(savedService -> processBanner(savedService, banner, null));
                            });
                })

                .switchIfEmpty(Mono.defer(() -> badRequest("Empresa não encontrada.")));
    }

    // 🔹 Processa banner do serviço
    private Mono<ResponseEntity<ApiResponse<Service>>> processBanner(Service service, Mono<FilePart> bannerMono, String message) {
        String basePath = fileConfig.getService(service.getId());
        File dir = new File(basePath);
        if (!dir.exists() && !dir.mkdirs()) {
            return internalError("Não foi possível criar diretório para salvar imagens.");
        }

        String bannerPath = basePath + "/banner.png";

        return bannerMono
                .flatMap(imageService::validateAndProcessImage)
                .flatMap(imageBytes -> {
                    try {
                        Files.write(Path.of(bannerPath), imageBytes);
                        if(message != null){
                            return success(message, service);
                        }
                        return success("Serviço criado com sucesso.", service);
                    } catch (IOException e) {
                        return internalError("Erro ao salvar banner.");
                    }
                });
    }

    // 🔹 Helpers para respostas
    private Mono<ResponseEntity<ApiResponse<Service>>> badRequest(String message) {
        return Mono.just(ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null)));
    }

    private Mono<ResponseEntity<ApiResponse<Service>>> internalError(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, message, null)));
    }

    private Mono<ResponseEntity<ApiResponse<Service>>> success(String message, Service data) {
        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, message, data)));
    }
}
