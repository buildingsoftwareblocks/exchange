package com.btb.exchange.shared.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

/**
 * Log all application properties
 */
@Slf4j
public class PropertyLogger implements ApplicationListener<ApplicationPreparedEvent> {

    private static final AtomicBoolean RUN_ONCE = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        if (!RUN_ONCE.getAndSet(true)) {
            logEnvironmentDetails(event.getApplicationContext().getEnvironment());
        }
    }

    private void logEnvironmentDetails(final Environment env) {
        log.info("====== Environment and configuration ======");
        logActiveProfiles(env);
        logPropertySources(env);
        logProperties(env);
        log.info("===========================================");
    }

    private void logActiveProfiles(final Environment env) {
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
    }

    private void logPropertySources(final Environment env) {
        MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        String[] sourceNames = sources.stream()
                .filter(EnumerablePropertySource.class::isInstance)
                .map(PropertySource::getName)
                .toArray(String[]::new);
        log.info("Property sources: {}", Arrays.toString(sourceNames));
    }

    private void logProperties(final Environment env) {
        MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .distinct()
                .sorted()
                .forEach(prop -> logProperty(env, prop));
    }

    private void logProperty(final Environment env, final String prop) {
        try {
            final String value = StringUtils.containsIgnoreCase(prop, "password") ? "*****" : env.getProperty(prop);
            log.info("{}={}", prop, value);
        } catch (Exception e) {
            log.warn("Error logging property {}: {}", prop, e.getMessage());
        }
    }
}
