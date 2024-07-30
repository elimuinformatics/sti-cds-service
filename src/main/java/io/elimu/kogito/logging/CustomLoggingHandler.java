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

import lombok.Data;
import org.jboss.logmanager.handlers.ConsoleHandler;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class CustomLoggingHandler extends ConsoleHandler {

  List<Pattern> regex;
  String mask;

  @Override
  public void publish(LogRecord logrecord) {
    if(logrecord.getParameters()!=null){
      logrecord.setMessage(String.format(logrecord.getMessage(), logrecord.getParameters()));
    }
    logrecord.setMessage(maskPaterns(logrecord.getMessage()));
    super.publish(logrecord);
  }

  public String maskPaterns(String body) {
    for (Pattern pattern : regex) {
      Matcher matcher = pattern.matcher(body);
      body = matcher.replaceAll(mask);
    }
    return body;
  }
}
