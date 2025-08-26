package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "companies")
public class Company {

    @Id
    private String id;
    private String name = ""; // Nome único da empresa
    private String email = ""; // Email corporativo único
    private String phone = "";
    private String address = "";
    private Map<String, String> metadata = new HashMap<>();
    private CompanyType type = CompanyType.OTHER; // Tipo da empresa (ex: SALON, BARBERSHOP)
    private boolean active = true;
    private GeoLocation location = new GeoLocation(0.0, 0.0); // Dados de geolocalização
    private String owner = ""; // Dono principal da empresa
    private List<String> managers = new ArrayList<>(); // Gestores (acesso administrativo)
    private List<String> staff = new ArrayList<>(); // Funcionários que prestam serviços
    @CreatedDate
    private Instant createdAt = Instant.now();
    @LastModifiedDate
    private Instant updatedAt = Instant.now();
}
