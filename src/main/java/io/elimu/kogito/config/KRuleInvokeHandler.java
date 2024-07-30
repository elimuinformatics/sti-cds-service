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

import java.util.Map;
import java.util.HashMap;

import io.elimu.kogito.util.KRuleInvokeHelper;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.elimu.kogito.model.NamedDataObject;

public class KRuleInvokeHandler implements KogitoWorkItemHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KRuleInvokeHandler.class);
    private static final String PID_KEY = "processInstanceId";

    private final KRuleInvokeHelper helper;

    public KRuleInvokeHandler(String kbaseName) {
        this.helper = new KRuleInvokeHelper(kbaseName);
    }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        //do nothing
    }

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        Map<String, Object> facts = new HashMap<>();
        workItem.getParameters().keySet().stream().filter((key) -> {
            return key.startsWith("fact_");
        }).forEach((key) -> {
            facts.put(key.substring(5), workItem.getParameter(key));
        });
        workItem.getParameters().keySet().stream().filter((key) -> {
            return key.startsWith("namedfact_");
        }).forEach((key) -> {
            facts.put(key.substring(10), new NamedDataObject(key.substring(10), workItem.getParameter(key)));
        });
        if (!facts.containsKey(PID_KEY)) {
            facts.put(PID_KEY, new NamedDataObject(PID_KEY, workItem.getProcessInstanceStringId()));
        }
        String ruleFlowGroup = (String)workItem.getParameter("ruleFlowGroup");
        String globalVar = (String)workItem.getParameter("inferencesGlobalVariableName");
        String sRuleLimit = (String)workItem.getParameter("ruleLimit");
        Integer ruleLimit = null;
        if (sRuleLimit != null) {
            ruleLimit = Integer.valueOf(sRuleLimit);
        }
        if (ruleLimit == null) {
            ruleLimit = 100000;
        } else if (ruleLimit <= 0) {
            ruleLimit = null;
        }
        if (ruleFlowGroup == null) {
            LOG.error("Cannot have empty ruleFlowGroup pararameter");
            throw new RuntimeException("Cannot have empty ruleFlowGroup pararameter");
        } else {
            if (globalVar == null) {
                globalVar = "inferences";
            }
	    Map<String, Object> results = workItem.getResults();
	    results.putAll(helper.invokeRules(facts, ruleFlowGroup, globalVar, ruleLimit));
            manager.completeWorkItem(workItem.getStringId(), results);
            LOG.trace("KRuleInvokeHanlder is completed");
        }
    }
}

