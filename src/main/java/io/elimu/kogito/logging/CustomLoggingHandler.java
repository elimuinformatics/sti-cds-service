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
