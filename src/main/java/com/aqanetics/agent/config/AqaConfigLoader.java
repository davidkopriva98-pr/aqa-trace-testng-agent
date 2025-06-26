package com.aqanetics.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AqaConfigLoader {

  public static final String AGENT_API_ENDPOINT = "/agent";
  public static final String SUITE_API_ENDPOINT = "/suite-executions/";
  public static final String TEST_API_ENDPOINT = "/test-executions/";
  public static final String LOG_API_ENDPOINT = "/logs";
  public static final String ARTIFACTS_ENDPOINT = "/artifacts";
  public static final ObjectMapper OBJECT_MAPPER =
      (new ObjectMapper()).registerModule(new JavaTimeModule());
  private static final Logger LOGGER = LoggerFactory.getLogger(AqaConfigLoader.class);

  private static final List<ConfigSource> configSources =
      List.of(new EnvironmentConfigSource(), new FileConfigSource("aqa-trace-agent.properties"));

  public static String API_ENDPOINT;
  public static boolean ENABLED;

  static {
    loadProperties();
  }

  public AqaConfigLoader() {}

  private static void loadProperties() {

    API_ENDPOINT = getProperty("aqa-trace.server.hostname", null);
    ENABLED = getBooleanProperty("aqa-trace.enabled", false);
    LOGGER.info("Loaded properties.");
  }

  public static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
    return Boolean.parseBoolean(getProperty(propertyName, String.valueOf(defaultValue)));
  }

  public static String getProperty(String key, String defaultValue) {
    for (ConfigSource source : configSources) {
      Optional<String> value = source.getProperty(key);
      if (value.isPresent()) {
        LOGGER.debug(
            "Property '{}' found in {}: {}", key, source.getClass().getSimpleName(), value.get());
        return value.get();
      }
    }
    LOGGER.debug(
        "Property '{}' not found in any source, using default value: {}", key, defaultValue);
    return defaultValue;
  }
}
