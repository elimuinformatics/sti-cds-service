package io.elimu.kogito.util;

import java.util.Map;

import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class CustomOperationOutcome {

    private CustomOperationOutcome() {
    }

    public static Map<String, Object> createOperationOutcome(String code, String severity, String diagnostics)
            throws JsonProcessingException {
        OperationOutcome outcome = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent issue = outcome.addIssue();
        issue.setDiagnostics(diagnostics);
        issue.setCode(OperationOutcome.IssueType.valueOf(code.toUpperCase()));
        issue.setSeverity(OperationOutcome.IssueSeverity.valueOf(severity.toUpperCase()));
        String errorJson = FhirContext.forR4Cached().newJsonParser().encodeResourceToString(outcome);
        ObjectMapper mapper = new ObjectMapper();
        return (Map<String, Object>) mapper.readValue(errorJson, Map.class);
    }
}

