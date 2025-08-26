package project1.ares.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "system_mails")
public class Mail {
    private String id;
    private String mail;
    private String pass;
}
