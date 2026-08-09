package com.projectx.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 defaults to Jackson 3 (tools.jackson.*, an immutable JsonMapper) and only
 * auto-configures that JsonMapper bean when no ObjectMapper bean exists. Every DTO/annotation in
 * this codebase (@JsonIgnoreProperties etc.) targets Jackson 2 (com.fasterxml.jackson.*), so
 * defining this bean is what makes Boot's Jackson-3 autoconfiguration back off and puts the whole
 * app - including Spring MVC's own request/response conversion - on Jackson 2 instead.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
