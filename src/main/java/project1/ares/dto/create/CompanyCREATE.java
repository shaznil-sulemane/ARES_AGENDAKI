package project1.ares.dto.create;

import lombok.*;
import project1.ares.model.CompanyType;
import project1.ares.model.GeoLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CompanyCREATE {
    private String name; // Nome único da empresa
    private String email; // Email corporativo único
    private String phone;
    private String address;

    private Map<String, Object> metadata = new HashMap<>();
    private CompanyType type; // Tipo da empresa (ex: SALON, BARBERSHOP)
    private GeoLocation location; // Dados de geolocalização
}
