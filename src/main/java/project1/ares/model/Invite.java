package project1.ares.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "invites")
public class Invite {
    @Id
    private String id;
    private String identifier;
    private List<Channel> channels = new ArrayList<>();
    private String companyId;
    private Map<String, Object> metadata = new HashMap<>();
    private LocalDateTime expitesAt;
    private Status status = Status.PENDING;

    public enum Status {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    public enum Channel {
        WHATSAPP, EMAIL, SMS
    }
}
