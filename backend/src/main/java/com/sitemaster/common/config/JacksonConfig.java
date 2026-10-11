package com.sitemaster.common.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the global string-trimming deserializer. Spring Boot adds every
 * Jackson Module bean to the application's ObjectMapper automatically.
 * JSON bodies do not pass through @InitBinder, which is why trimming lives here.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Module stringTrimmingModule() {
        SimpleModule module = new SimpleModule("StringTrimmingModule");
        module.addDeserializer(String.class, new TrimmingStringDeserializer());
        return module;
    }
}
