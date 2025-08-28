package project1.ares.controller;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project1.ares.config.ApiResponse;
import project1.ares.config.CustomUserDetails;
import project1.ares.model.Category;
import project1.ares.model.Company;
import project1.ares.model.User;
import project1.ares.repository.CategoryRepository;
import project1.ares.repository.CompanyRepository;
import project1.ares.repository.UserRepository;
import project1.ares.service.SequenceGeneratorService;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public CategoryController(UserRepository userRepository, CompanyRepository companyRepository, CategoryRepository categoryRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<List<Category>>>> getByCompanyId(@PathVariable("companyId") String companyId, @AuthenticationPrincipal CustomUserDetails principal) {
        Mono<User> userMono = userRepository.findById(principal.getId());
        Mono<Company> companyMono = companyRepository.findById(companyId);

        return Mono.zip(userMono, companyMono)
                .flatMap(tuple -> {
                    if(!tuple.getT2().getOwner().equals(tuple.getT1().getId()) && !principal.getRole().equals(User.Role.ADMIN)) {
                        return Mono.error(new AuthorizationDeniedException("Access denied"));
                    }

                    return categoryRepository.findByCompanyId(companyId)
                            .collectList()
                            .flatMap(category -> Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Categorias encontradas com sucesso.", category))));

                });
    }

    @PostMapping("/{companyId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Category>>> createByCompanyId(@PathVariable("companyId") String companyId, @AuthenticationPrincipal CustomUserDetails principal, @RequestBody CategoryCREATE category) {
        Mono<User> userMono = userRepository.findById(principal.getId());
        Mono<Company> companyMono = companyRepository.findById(companyId);

        return Mono.zip(userMono, companyMono)
                .flatMap(tuple -> {
                    if(!tuple.getT2().getOwner().equals(tuple.getT1().getId()) && !principal.getRole().equals(User.Role.ADMIN)) {
                        return Mono.error(new AuthorizationDeniedException("Access denied"));
                    }

                    return sequenceGeneratorService.generateId("category", "CT", 8)
                            .flatMap(catID -> {
                                Category newCategory = new Category();
                                newCategory.setId(catID);
                                newCategory.setCompanyId(companyId);
                                newCategory.setName(category.getName());

                                return categoryRepository.save(newCategory)
                                        .flatMap(savedCat -> Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Categoria criada com sucesso.", savedCat))));

                            });
                });
    }

    @PutMapping("/{companyId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Category>>> updateByCompanyId(@PathVariable("companyId") String companyId, @AuthenticationPrincipal CustomUserDetails principal, @RequestBody CategoryUPDATE category) {
        Mono<User> userMono = userRepository.findById(principal.getId());
        Mono<Company> companyMono = companyRepository.findById(companyId);
        Mono<Category> categoryMono = categoryRepository.findById(category.getId());

        return Mono.zip(userMono, companyMono, categoryMono)
                .flatMap(tuple -> {
                    if(!tuple.getT2().getOwner().equals(tuple.getT1().getId()) && !principal.getRole().equals(User.Role.ADMIN)) {
                        return Mono.error(new AuthorizationDeniedException("Access denied"));
                    }

                    if(!tuple.getT3().getCompanyId().equals(companyId)) {
                        return Mono.error(new AuthorizationDeniedException("Access denied"));
                    }

                    Category updatedCategory = new Category();
                    updatedCategory.setId(category.getId());
                    updatedCategory.setName(category.getName());
                    updatedCategory.setCompanyId(tuple.getT3().getCompanyId());

                    return categoryRepository.save(updatedCategory)
                            .flatMap(savedCategory -> Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Categoria atualizada com sucesso.", savedCategory))));

                });
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteById(
            @PathVariable("categoryId") String categoryId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.just(new Category())) // evita null
                .flatMap(category -> {
                    if (category.getId() == null) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(new ApiResponse<>(false, "Categoria não encontrada.", null)));
                    }

                    // Admin pode apagar qualquer categoria
                    if (principal.getRole().equals(User.Role.ADMIN)) {
                        return categoryRepository.deleteById(categoryId)
                                .thenReturn(ResponseEntity.ok(new ApiResponse<>(true, "Categoria eliminada com sucesso.", null)));
                    }

                    // Se for manager, tem que ser dono da empresa
                    return companyRepository.findById(category.getCompanyId())
                            .flatMap(company -> {
                                if (!company.getOwner().equals(principal.getId())) {
                                    return Mono.just(ResponseEntity
                                            .status(HttpStatus.FORBIDDEN)
                                            .body(new ApiResponse<>(false, "Acesso negado.", null)));
                                }

                                return categoryRepository.deleteById(categoryId)
                                        .thenReturn(ResponseEntity.ok(new ApiResponse<>(true, "Categoria eliminada com sucesso.", null)));
                            });
                });
    }


    @Data
    public static class CategoryCREATE {
        private String name;
    }

    @Data
    public static class CategoryUPDATE {
        private String id;
        private String name;
    }

    @Data
    public static class CategoryDELETE {
        private String id;
    }

}
