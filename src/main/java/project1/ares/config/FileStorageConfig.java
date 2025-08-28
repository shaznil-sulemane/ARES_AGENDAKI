package project1.ares.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class FileStorageConfig {

    private final String basePath;

    public FileStorageConfig() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            this.basePath = "C:\\agendaki";
        } else {
            this.basePath = "/var/agendaki";
        }
    }

    public String getUserPath(String userId) {
        return basePath + "/users/" + userId;
    }
    public String getCompany(String companyId) {
        return basePath + "/companies/" + companyId;
    }
    public String getService(String serviceId) {
        return basePath + "/services/" + serviceId;
    }
}
