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

import org.hl7.fhir.r4.model.OperationOutcome;

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

