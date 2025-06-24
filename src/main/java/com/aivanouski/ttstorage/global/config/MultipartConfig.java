package com.aivanouski.ttstorage.global.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // Set unlimited file size for hundreds of GB support
        factory.setMaxFileSize(DataSize.ofBytes(-1));

        // Set unlimited request size
        factory.setMaxRequestSize(DataSize.ofBytes(-1));

        // Set file size threshold to 10MB (files larger than this will be written to
        // disk)
        factory.setFileSizeThreshold(DataSize.ofMegabytes(10));

        return factory.createMultipartConfig();
    }
}