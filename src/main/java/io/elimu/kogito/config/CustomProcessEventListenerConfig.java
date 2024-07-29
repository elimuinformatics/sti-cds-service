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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.kie.kogito.process.impl.DefaultProcessEventListenerConfig;

@ApplicationScoped
public class CustomProcessEventListenerConfig extends DefaultProcessEventListenerConfig {

    public CustomProcessEventListenerConfig() {
    }
    
    @PostConstruct
    public void initialize() {
        register(new ConfigAwareProcessEventListener());
    }
}
