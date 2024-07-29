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

package io.elimu.kogito.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.drools.compiler.kie.builder.impl.KieServicesImpl;
import org.drools.core.common.InternalAgenda;

import org.kie.api.runtime.KieSession;

public class KRuleInvokeHelper {

    private static final Logger LOG = LoggerFactory.getLogger(KRuleInvokeHelper.class);

    private final String kbaseName;
    private final KieContainer kcontainer;

    public KRuleInvokeHelper(String kbaseName) {
        this.kbaseName = kbaseName;
	((KieServicesImpl) KieServices.get()).nullKieClasspathContainer();
        this.kcontainer = KieServices.Factory.get().getKieClasspathContainer(Thread.currentThread().getContextClassLoader());
    }

    public Map<String, Object> invokeRules(Map<String, Object> facts, String ruleFlowGroup) {
	    return invokeRules(facts, ruleFlowGroup, "inferences", 100000);
    }

    public Map<String, Object> invokeRules(Map<String, Object> facts, String ruleFlowGroup, String globalVar, Integer ruleLimit) {
        if (ruleFlowGroup == null) {
            LOG.error("Cannot have empty ruleFlowGroup pararameter");
            throw new RuntimeException("Cannot have empty ruleFlowGroup pararameter");
        } else {
            if (globalVar == null) {
                globalVar = "inferences";
            }
            KieSession ksession = kcontainer.getKieBase(kbaseName).newKieSession();
            facts.values().forEach((object) -> { ksession.insert(object); });
            ((InternalAgenda)ksession.getAgenda()).activateRuleFlowGroup(ruleFlowGroup);
            try {
                Map<String, Object> inferences = new HashMap<>();
                ksession.setGlobal(globalVar, inferences);
                long rulesFired = 0L;
                if (ruleLimit > 0) {
                    rulesFired = (long)ksession.fireAllRules(ruleLimit);
                    if (rulesFired == (long)ruleLimit) {
                        LOG.warn("Rule firing ended due to rule limit (" + ruleLimit + " rules) rather than completion. Possible infinite loop in rules");
                    }
                } else {
                    ksession.fireAllRules();
                }
                LOG.debug("After rule firing, inferences has " + inferences.size() + " elements");
                Map<String, Object> results = new HashMap<>();
                Map.Entry<String, Object> entry;
                for(Iterator<Map.Entry<String, Object>> iter = inferences.entrySet().iterator(); iter.hasNext(); results.put("inference_" + (String)entry.getKey(), entry.getValue())) {
                    entry = iter.next();
                    if (entry.getValue() != null) {
                        LOG.debug("Adding to the results [inference_" + entry.getKey() + " with object of type " + entry.getValue().getClass().getName() + "]");
                    } else {
                       LOG.debug("Adding to the results [inference_" + entry.getKey() + " with NULL]");
                    }
                }
                inferences.forEach((key, value) -> { results.put("inference_" + key, value); });
                LOG.trace("KRuleInvokeHelper is completed");
                return results;
            } catch (Exception ex) {
                LOG.error("Problem invoking rules", ex);
                throw new RuntimeException("Problem invoking rules", ex);
            } finally {
                ksession.dispose();
            }
        }
    }
}

