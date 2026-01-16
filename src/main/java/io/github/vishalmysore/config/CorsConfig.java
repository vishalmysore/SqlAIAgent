package io.github.vishalmysore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins
        config.setAllowedOriginPatterns(Collections.singletonList("*"));

        // Allow all headers
        config.setAllowedHeaders(Collections.singletonList("*"));

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow credentials
        config.setAllowCredentials(true);

        // How long the response from a pre-flight request can be cached
        config.setMaxAge(3600L);

        // Apply CORS configuration to all paths
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
