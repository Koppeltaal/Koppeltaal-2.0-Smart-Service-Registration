package nl.koppeltaal.smartserviceregistration.config;

import nl.koppeltaal.smartserviceregistration.interceptor.LoggedInInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggedInInterceptor loggedInInterceptor;

    public WebMvcConfig(LoggedInInterceptor loggedInInterceptor) {
        this.loggedInInterceptor = loggedInInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggedInInterceptor);
    }
}