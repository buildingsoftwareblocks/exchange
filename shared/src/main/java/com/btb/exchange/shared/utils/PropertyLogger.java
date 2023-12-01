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
class PropertyLogger implements ApplicationListener<ApplicationPreparedEvent> {

    private static final AtomicBoolean RUN_ONCE = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        if (!RUN_ONCE.getAndSet(true)) {
            show((event.getApplicationContext().getEnvironment()));
        }
    }

    private void show(Environment env) {
        log.info("====== Environment and configuration ======");
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        log.info(
                "Property sources: {}",
                Arrays.toString(sources.stream()
                        .filter(EnumerablePropertySource.class::isInstance)
                        .map(PropertySource::getName)
                        .toArray()));

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
