package com.aqanetics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
  private static final Properties properties = new Properties();
  public static String API_ENDPOINT;
  public static boolean ENABLED;

  static {
    loadProperties();
  }

  public AqaConfigLoader() {}

  private static void loadProperties() {
    try (InputStream input =
        AqaConfigLoader.class.getClassLoader().getResourceAsStream("aqa-trace-agent.properties")) {
      if (input != null) {
        properties.load(input);
        API_ENDPOINT = properties.getProperty("aqa-trace.server.hostname", null);
        ENABLED = Boolean.parseBoolean(properties.getProperty("aqa-trace.enabled", "false"));
        LOGGER.info("Loaded configuration from aqa-agent.properties.");
      } else {
        LOGGER.warn("Could not find aqa-agent.properties. Using default values.");
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to load aqa-agent.properties. {}", e.getMessage());
    }
  }

  public static String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
