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

