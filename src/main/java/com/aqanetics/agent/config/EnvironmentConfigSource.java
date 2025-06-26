package com.aqanetics.agent.config;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentConfigSource.class);

  public EnvironmentConfigSource() {}

  @Override
  public Optional<String> getProperty(String key) {
    return Optional.ofNullable(System.getenv(convertKey(key)));
  }

  private String convertKey(String key) {
    return key.replace(".", "_").replace("-", "_").toUpperCase();
  }
}
