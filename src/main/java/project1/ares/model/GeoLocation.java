package project1.ares.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocation {
    private double latitude;
    private double longitude;
}
