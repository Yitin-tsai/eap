package com.eap.ai.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Configuration properties for EAP MCP client.
 */
@Data
@Component
@ConfigurationProperties(prefix = "eap.mcp")
@Validated
public class EapMcpProperties {

    @NotBlank
    private String baseUrl;

    private String basePath;

    @NotBlank
    private String ssePath;

    @NotBlank
    private String messagePath;

    @Min(1)
    private int timeoutSeconds;

    public Duration getTimeoutDuration() {
        return Duration.ofSeconds(Math.max(1, timeoutSeconds));
    }

    public String getSseUrl() {
        return joinPaths(baseUrl, basePath, ssePath);
    }

    public String getMessageUrl() {
        return joinPaths(baseUrl, basePath, messagePath);
    }

    private String joinPaths(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            String normalized = p;
            if (sb.length() == 0) {
                sb.append(normalized.replaceAll("/+$", ""));
            } else {
                sb.append("/").append(normalized.replaceAll("^/+|/+$", ""));
            }
        }
        return sb.toString();
    }
}
