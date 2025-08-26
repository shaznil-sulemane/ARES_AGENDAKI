package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "staffs")
public class Staff {
    @Id
    private String id;
    private String userId;  // vínculo ao User existente
    private String companyId;  // vínculo à empresa
    private List<String> services;  // Ex: HAIRDRESSER, NAIL_ARTIST, RECEPCIONIST
    private boolean active;
    private String specialization; // opcional: corte, manicure, etc.
    private String shift;          // opcional: manhã, tarde, noite
    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
