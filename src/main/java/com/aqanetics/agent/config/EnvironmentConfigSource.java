package com.aqanetics.agent.config;

import java.util.Optional;

public class EnvironmentConfigSource implements ConfigSource {

  public EnvironmentConfigSource() {}

  @Override
  public Optional<String> getProperty(String key) {
    return Optional.ofNullable(System.getenv(convertKey(key)));
  }

  private String convertKey(String key) {
    return key.replace(".", "_").replace("-", "_").toUpperCase();
  }
}
