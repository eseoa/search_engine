package com.github.eseoa.searchEngine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties()
@Component
@Data

public class Configuration {

    private Map<String, String> sites;
    private String userAgent;
}
