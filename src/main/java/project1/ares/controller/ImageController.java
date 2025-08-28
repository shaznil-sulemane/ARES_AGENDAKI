package project1.ares.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project1.ares.config.FileStorageConfig;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageConfig storageConfig;

    private Mono<ResponseEntity<Resource>> serveFile(Path filePath) {
        return Mono.fromCallable(() -> {
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        });
    }

    // --- USERS ---
    @GetMapping("/user/{id}/{filename}")
    public Mono<ResponseEntity<Resource>> getUserImage(
            @PathVariable String id,
            @PathVariable String filename) {
        Path filePath = Paths.get(storageConfig.getUserPath(id), filename);
        return serveFile(filePath);
    }

    // --- COMPANIES ---
    @GetMapping("/company/{id}/logo")
    public Mono<ResponseEntity<Resource>> getCompanyLogo(@PathVariable String id) {
        Path filePath = Paths.get(storageConfig.getCompany(id), "/logo.png");
        return serveFile(filePath);
    }

    @GetMapping("/company/{id}/banner")
    public Mono<ResponseEntity<Resource>> getCompanyBanner(@PathVariable String id) {
        Path filePath = Paths.get(storageConfig.getCompany(id), "/banner.png");
        return serveFile(filePath);
    }

    // --- SERVICES ---
    @GetMapping("/service/{id}/banner")
    public Mono<ResponseEntity<Resource>> getServiceImage(@PathVariable String id) {
        Path filePath = Paths.get(storageConfig.getService(id), "/banner.png");
        return serveFile(filePath);
    }
}
