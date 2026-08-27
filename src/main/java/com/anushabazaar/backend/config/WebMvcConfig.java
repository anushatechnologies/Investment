package com.anushabazaar.backend.config;

import com.anushabazaar.backend.domain.DomainEnums;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, DomainEnums.PaymentMode.class, DomainEnums.PaymentMode::fromString);
    }
}
