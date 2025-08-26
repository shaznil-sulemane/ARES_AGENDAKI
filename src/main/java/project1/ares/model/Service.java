package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "services")
public class Service {
    @Id
    private String id;

    private String companyId;   // FK para a Company (salão)
    private String name;
    private String description;

    private BigDecimal price;
    private int durationMinutes;

    private boolean active;
}
