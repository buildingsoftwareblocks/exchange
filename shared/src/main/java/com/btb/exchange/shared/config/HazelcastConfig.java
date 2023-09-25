package com.btb.exchange.shared.config;

import com.hazelcast.core.HazelcastInstance;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component("shared.hazelcastConfig")
@Slf4j
public class HazelcastConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @PreDestroy
    public void destroy() {
        try {
            HazelcastInstance instance = applicationContext.getBean(HazelcastInstance.class);
            log.info("HazelcastInstance shutdown");
            instance.shutdown();
        } catch (BeansException e) {
            log.warn("HazelcastInstance is NULL");
        }
    }
}
