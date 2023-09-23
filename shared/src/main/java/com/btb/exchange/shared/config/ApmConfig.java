package com.btb.exchange.shared.config;

import java.net.URL;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.bull.javamelody.MonitoringFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApmConfig {

    @Value("${exchange.apm.enabled:false}")
    private boolean enabled;

    @Value("${exchange.apm.server:}")
    private String server;

    @Value("${exchange.apm.node:http://${spring.cloud.client.ip-address:}:${server.port:}}")
    private String node;

    @Value("${spring.application.name:}")
    private String applicationName;

    /** Use event listener instead of PostConstruct to support lazy loaded applications as well */
    @EventListener
    public void postConstruct(ContextRefreshedEvent ignoredEvent) {
        if (enabled) {
            try {
                final URL collectServerUrl = new URL(server);
                final URL applicationNodeUrl = new URL(node);
                log.info("Connect ({}) to : {}", applicationNodeUrl, collectServerUrl);
                MonitoringFilter.registerApplicationNodeInCollectServer(
                        applicationName, collectServerUrl, applicationNodeUrl);
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
