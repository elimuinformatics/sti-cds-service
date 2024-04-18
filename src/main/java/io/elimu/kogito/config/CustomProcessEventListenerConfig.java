package io.elimu.kogito.config;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.process.impl.DefaultProcessEventListenerConfig;

@ApplicationScoped
public class CustomProcessEventListenerConfig extends DefaultProcessEventListenerConfig {

    public CustomProcessEventListenerConfig() {
    }
    
    @PostConstruct
    public void initialize() {
        register(new ConfigAwareProcessEventListener());
    }
}
