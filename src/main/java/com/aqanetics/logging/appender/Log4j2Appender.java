package com.aqanetics.logging.appender;

import com.aqanetics.AqaConfigLoader;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.agent.testng.dto.TestExecutionLogDto;
import com.aqanetics.utils.CrudMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "Log4j2Appender", category = "Core", elementType = "appender", printObject = true)
public class Log4j2Appender extends AbstractAppender {

  private static final boolean ENABLED_LOGGING =
      Objects.equals(AqaConfigLoader.getProperty("aqa-trace.logging.enabled", "false"), "true");
  private static final String[] LOGS_ONLY_FROM_PACKAGES =
      AqaConfigLoader.getProperty("aqa-trace.logging.only-from-package", "").split(",");

  private static final Function<LogEvent, TestExecutionLogDto> CONVERTER =
      (e) ->
          new TestExecutionLogDto(
              e.getMessage().getFormattedMessage(),
              e.getLevel().toString(),
              Instant.ofEpochMilli(e.getInstant().getEpochMillisecond()));

  protected Log4j2Appender(String name, Layout<?> layout) {
    super(name, null, layout, false, null);
  }

  @PluginFactory
  public static Appender createAppender(
      @PluginAttribute("name") String name, @PluginElement("Layout") Layout<?> layout) {
    if (layout == null) {
      layout = PatternLayout.createDefaultLayout();
    }

    return new Log4j2Appender(name, layout);
  }

  public void append(LogEvent event) {
    if (ENABLED_LOGGING
        && AqaConfigLoader.API_ENDPOINT != null
        && !event.getLoggerName().startsWith("com.aqanetics.agent.testng")
        && ExecutionEntities.inProgressTestExecutionId != null
        && (LOGS_ONLY_FROM_PACKAGES.length == 0
            || Arrays.stream(LOGS_ONLY_FROM_PACKAGES)
                .anyMatch((s) -> event.getLoggerName().startsWith(s)))) {
      try {
        CrudMethods.postLog(
            AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(CONVERTER.apply(event)));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
