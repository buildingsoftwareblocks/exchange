package com.btb.exchange.shared.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.StreamSupport;

/**
 * Log all application properties
 */
@Slf4j
@Component
public class PropertyLogger {

    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext().getEnvironment();
        log.info("====== Environment and configuration ======");
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        log.info("Property sources: {}", Arrays.toString(sources.stream()
                .filter(EnumerablePropertySource.class::isInstance)
                .map(PropertySource::getName).toArray()
        ));

        StreamSupport.stream(sources.stream().spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .distinct()
                .sorted()
                .forEach(prop -> {
                    try {
                        if (!(StringUtils.containsIgnoreCase(prop, "password"))) {
                            log.info("{}={}", prop, env.getProperty(prop));
                        } else {
                            log.info("{} = *****", prop);
                        }
                    } catch (Exception e) {
                        log.warn("{} -> {}", prop, e.getMessage());
                    }
                });
        log.info("===========================================");
    }
}