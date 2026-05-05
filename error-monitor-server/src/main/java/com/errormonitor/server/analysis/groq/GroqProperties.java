package com.errormonitor.server.analysis.groq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {
    private String apiKey = "";
    private String model = "llama-3.3-70b-versatile";
    private int maxTokens = 4096;
    private int maxToolRounds = 10;
}
