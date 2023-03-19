package com.btb.exchange.shared.config;

import lombok.extern.slf4j.Slf4j;
import net.bull.javamelody.MonitoringFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.URL;

@Component
@Slf4j
public class ApmConfig {

    @Value("${cnl.apm.enabled:false}")
    private boolean enabled;

    @Value("${cnl.apm.server:}")
    private String server;

    @Value("${cnl.apm.node:}")
    private String node;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Use event listener instead of PostContruct to support lazy loaded applications as well
     */
    @EventListener
    public void postConstruct(ContextRefreshedEvent event) {
        if (enabled) {
            try {
                final URL collectServerUrl = new URL(server);
                final URL applicationNodeUrl = new URL(node);
                log.info("Connect ({}) to : {}", applicationNodeUrl, collectServerUrl);
                MonitoringFilter.registerApplicationNodeInCollectServer(applicationName, collectServerUrl, applicationNodeUrl);
            } catch (Throwable e) {
                log.warn("Exception", e);
            }
        }
    }

    @PreDestroy
    public void preDestruct() {
        if (enabled) {
            try {
                MonitoringFilter.unregisterApplicationNodeInCollectServer();
            } catch (Throwable e) {
                log.warn("Exception", e);
            }
        }
    }
}
