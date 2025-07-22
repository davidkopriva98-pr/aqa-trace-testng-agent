package com.aqanetics.agent.config;

import java.util.Optional;

public class SystemConfigSource implements ConfigSource {

  public SystemConfigSource() {}

  @Override
  public Optional<String> getProperty(String key) {
    return Optional.ofNullable(System.getProperty(key));
  }
}
