package project1.ares.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "categories")
public class Category {
    private String id;
    private String companyId;
    private String name;
}
