package com.activitiesclub.activitiesclub_backend;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Path uploadsDir;

    public WebConfig(@Value("${app.uploads.dir}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(this.uploadsDir.toUri().toString());
    }
}
