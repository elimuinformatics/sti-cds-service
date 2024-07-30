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

package io.elimu.kogito.logging;

import io.quarkus.runtime.Startup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.logging.Handler;
import java.util.regex.Pattern;

import static io.quarkus.bootstrap.logging.InitialConfigurator.DELAYED_HANDLER;

@ApplicationScoped
@Slf4j
@Startup(1)
@Data
public class LoggerInitializer {

  @ConfigProperty(name = "io.elimu.log-scrubber")
  Optional<Map<String, String>> logConfigProperty;

  Map<String, String> logConfig;
  List<Pattern> values = new ArrayList<>();

  @PostConstruct
  public void initLogger() {
    logConfig = logConfigProperty.orElse(new HashMap<>());
    parseLogConfig(logConfig);
    log.debug("Loading mask {}", logConfig.get("mask"));
    Handler[] oldHandlers = DELAYED_HANDLER.clearHandlers();

    CustomLoggingHandler customHandler = new CustomLoggingHandler();
    customHandler.setMask(logConfig.get("mask"));
    customHandler.setRegex(values);

    DELAYED_HANDLER.addHandler(customHandler);
    for (Handler oldHandler : oldHandlers) {
      DELAYED_HANDLER.addHandler(oldHandler);
    }
  }

  public void parseLogConfig(Map<String, String> logConfig) {
    for (Map.Entry<String, String> k : logConfig.entrySet()) {
      if (k.getKey().endsWith("]")) {
        log.debug("Loading masking Regex {}", k.getValue());
        values.add(Pattern.compile(k.getValue(), Pattern.CASE_INSENSITIVE));
      }
    }
  }
}
