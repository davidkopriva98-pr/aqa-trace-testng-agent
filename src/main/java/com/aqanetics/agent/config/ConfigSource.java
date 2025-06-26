package com.aqanetics.agent.config;

import java.util.Optional;

public interface ConfigSource {
  Optional<String> getProperty(String key);
}
