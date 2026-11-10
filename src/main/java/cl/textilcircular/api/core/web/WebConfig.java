package cl.textilcircular.api.core.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Every feature controller lives under the contract's base path; actuator and docs stay at the root. */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    public static final String API_BASE_PATH = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_BASE_PATH, HandlerTypePredicate.forBasePackage("cl.textilcircular.api.feature"));
    }
}
