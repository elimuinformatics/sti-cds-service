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

import java.util.HashMap;
import java.util.Map;

import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;

public class ImprovedCachingTerminologyProviderDecorator implements TerminologyProvider {

    private Map<String, Iterable<Code>> valueSetIndexById = new HashMap<>();

    private TerminologyProvider innerProvider;

    public ImprovedCachingTerminologyProviderDecorator(TerminologyProvider terminologyProvider) {
    	this.innerProvider = terminologyProvider;
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        if (!valueSetIndexById.containsKey(valueSet.getId())) {
            this.expand(valueSet);
        }

        Iterable<Code> codes = valueSetIndexById.get(valueSet.getId());

        if (codes == null) {
            return false;
        }
        for (Code c : codes) {
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
    	String originalId = valueSet.getId();
        if (!valueSetIndexById.containsKey(originalId)) {
        	Iterable<Code> codes = this.innerProvider.expand(valueSet);
            valueSetIndexById.put(originalId, codes);
            valueSetIndexById.put(valueSet.getId(), codes);
        }
        return valueSetIndexById.get(valueSet.getId());
    }

    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) {
        return this.innerProvider.lookup(code, codeSystem);
    }
}
