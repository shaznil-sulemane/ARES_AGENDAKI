package project1.ares.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "logs")
public class Log {
    @Id
    private String id;
    private String user;
    private String message;
    private Instant date = Instant.now();
}
