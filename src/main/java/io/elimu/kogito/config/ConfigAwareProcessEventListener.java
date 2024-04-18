package io.elimu.kogito.config;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;

/**
 * Makes sure every process has a fresh copy of the config information
 */
public class ConfigAwareProcessEventListener extends DefaultKogitoProcessEventListener {

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
        Config configRaw = ConfigProvider.getConfig();
        for (String p : configRaw.getPropertyNames()) {
            if (p.startsWith("configs")) {
                StringTokenizer st = new StringTokenizer(p, "\\.");
                List<String> l = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    l.add(st.nextToken());
                }
                if (l.get(1).equals("procvar")) {
                    instance.setVariable(l.get(2), configRaw.getConfigValue(p).getValue());
                }
            }
        }
    }
}
