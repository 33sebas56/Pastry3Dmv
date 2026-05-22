package com.backend.pastry3d.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${storage.uploads:uploads/models}")
    private String uploadsPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedPath = uploadsPath.endsWith("/") ? uploadsPath : uploadsPath + "/";
        registry.addResourceHandler("/uploads/models/**")
                .addResourceLocations("file:" + normalizedPath);
    }
}
