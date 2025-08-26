package project1.ares.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project1.ares.config.ApiResponse;
import project1.ares.config.CustomUserDetails;
import project1.ares.config.GlobalExceptionHandler;
import project1.ares.dto.create.CompanyCREATE;
import project1.ares.model.Company;
import project1.ares.model.CompanyType;
import project1.ares.repository.CompanyRepository;
import project1.ares.service.SequenceGeneratorService;
import project1.ares.util.Regex;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);
    private final CompanyRepository companyRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public CompanyController(CompanyRepository companyRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.companyRepository = companyRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @GetMapping
    public Flux<Company> getAll() {
        return companyRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Company>> getById(@PathVariable String id) {
        return companyRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Mono<ResponseEntity<ApiResponse<?>>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CompanyCREATE req
    ) {
        Company company = new Company();
        company.setOwner(principal.getId());
        company.setName(req.getName());
        company.setEmail(req.getEmail());
        company.setPhone(req.getPhone());
        company.setAddress(req.getAddress());
        company.setType(req.getType());
        //company.setType(CompanyType.valueOf(((String) body.getOrDefault("type", "OTHER")).toUpperCase()));
        company.setActive(true);

        return sequenceGeneratorService.generateCompanyId()
                .flatMap(compId -> {
                    log.info("Creating new company {}", compId);
                    company.setId(compId);
                    return companyRepository.save(company)
                            .map(savedCompany -> {
                                return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(false, "Empresa criada com sucesso.", savedCompany));
                            });
                });
    }

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
}
