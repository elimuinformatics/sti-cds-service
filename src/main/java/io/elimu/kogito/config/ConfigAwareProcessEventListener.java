// Copyright 2018-2024 Elimu Informatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
