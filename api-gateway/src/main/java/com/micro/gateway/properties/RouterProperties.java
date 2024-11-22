package com.micro.gateway.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "gateway")
@Data
public class RouterProperties {
    private String environment;
    private Map<String, RoutingInfo> route;
    private boolean skipAuthEnabled;


    @Data
    public static class RoutingInfo {
        private String endpoint;
        private String path;
        private String id;
    }
}
