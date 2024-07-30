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

package io.elimu.kogito.cql;

import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;

import java.util.List;

import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class CustomFhirRestRetrieveProviderFactory implements TypedRetrieveProviderFactory {

    FhirContext fhirContext;
    ClientFactory clientFactory;

    public CustomFhirRestRetrieveProviderFactory(FhirContext fhirContext, ClientFactory clientFactory){
        this.fhirContext = fhirContext;
        this.clientFactory = clientFactory;
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_REST;
    }

    @Override
    public RetrieveProvider create(String url, List<String> headers) {
       IGenericClient fhirClient = this.clientFactory.create(url, headers);
       return new CustomRestFhirRetrieveProvider(new SearchParameterResolver(this.fhirContext), fhirClient);
    }


}
