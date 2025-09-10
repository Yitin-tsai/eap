package com.eap.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("service")
    private String service;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("dependencies")
    private Map<String, String> dependencies;
    
    @JsonProperty("uptime")
    private String uptime;
    
    public static HealthResponse up(String service, String version) {
        return HealthResponse.builder()
                .status("UP")
                .service(service)
                .version(version)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HealthResponse down(String service, String version, String reason) {
        return HealthResponse.builder()
                .status("DOWN")
                .service(service)
                .version(version)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
