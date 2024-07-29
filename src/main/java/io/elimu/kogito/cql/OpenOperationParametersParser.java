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

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.OperationParametersParser;

public class OpenOperationParametersParser extends OperationParametersParser {

	public OpenOperationParametersParser(AdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
		super(adapterFactory, fhirTypeConverter);
	}

	@Override
	public IBaseDatatype getValueChild(IBaseParameters parameters, String name) {
		return super.getValueChild(parameters, name);
	}
	
	@Override
	public IBaseResource getResourceChild(IBaseParameters parameters, String name) {
		return super.getResourceChild(parameters, name);
	}
}
