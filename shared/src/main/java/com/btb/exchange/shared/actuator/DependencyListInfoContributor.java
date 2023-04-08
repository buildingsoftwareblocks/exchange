package com.btb.exchange.shared.actuator;

import org.cyclonedx.parsers.JsonParser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class DependencyListInfoContributor implements InfoContributor, InitializingBean {
    private final Resource bomFile;
    private final JsonParser jsonParser = new JsonParser();
    private @Nullable List<Dependency> dependencies;

    DependencyListInfoContributor(@Value("classpath:bom.json") Resource bomFile) {
        this.bomFile = bomFile;
    }

    @Override
    public void contribute(Info.Builder builder) {
        if (dependencies != null) {
            builder.withDetail("dependencies", dependencies);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (bomFile.exists()) {
            try (var is = bomFile.getInputStream()) {
                var bom = jsonParser.parse(is);
                this.dependencies =
                        bom.getComponents().stream().map(Dependency::new).toList();
            }
        }
    }

    record Dependency(String groupId, String artifactId, String version) {
        Dependency(org.cyclonedx.model.Component component) {
            this(component.getGroup(), component.getName(), component.getVersion());
        }
    }
}
