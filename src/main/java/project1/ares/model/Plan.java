package project1.ares.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "plans")
public class Plan {
    private String id;
    private String name = "";
    private BigDecimal price = BigDecimal.ZERO;
    private String title = "";
    private Duration duration = Duration.MENSAL;
    private String description = "";
    private boolean active = true;
    private List<String> features = new ArrayList<>();
    private String badge;
    private int position = 0;

    public enum Duration {
        DIARIA, SEMANAL, MENSAL, ANUAL
    }
}
