package io.elimu.kogito.config;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

@ApplicationScoped
public class CustomWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {

    @PostConstruct
    public void initialize() {
            register("RuleInvoke", new KRuleInvokeHandler("gc_cds_hooks"));
            register("DeserializeHooksRequest", new KDeserializeHookReqHandler());
            register("PlanDefinition", new PlanDefInlineHandler());
    }

    
}
