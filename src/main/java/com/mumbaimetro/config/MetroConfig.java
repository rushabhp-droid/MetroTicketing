package com.mumbaimetro.config;

import com.mumbaimetro.service.MetroService;
import com.mumbaimetro.threads.PeakHourMonitorThread;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MetroConfig implements WebMvcConfigurer {

    @Bean
    public MetroService metroService() {
        return new MetroService();
    }

    @Bean(destroyMethod = "interrupt")
    public PeakHourMonitorThread peakHourMonitorThread() {
        PeakHourMonitorThread thread = new PeakHourMonitorThread((isPeakHour, reason) -> {
            System.out.println("[PEAK-HOUR MONITOR] " + reason);
        });
        thread.start();
        return thread;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
