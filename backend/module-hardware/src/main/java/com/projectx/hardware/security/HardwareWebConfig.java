package com.projectx.hardware.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Scopes DeviceApiKeyAuthInterceptor to exactly the hardware ingestion endpoints. */
@Configuration
@EnableConfigurationProperties(HardwareRateLimitProperties.class)
public class HardwareWebConfig implements WebMvcConfigurer {

    private final DeviceApiKeyAuthInterceptor deviceApiKeyAuthInterceptor;

    public HardwareWebConfig(DeviceApiKeyAuthInterceptor deviceApiKeyAuthInterceptor) {
        this.deviceApiKeyAuthInterceptor = deviceApiKeyAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deviceApiKeyAuthInterceptor).addPathPatterns("/hardware/**");
    }
}
