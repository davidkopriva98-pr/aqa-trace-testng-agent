package com.aqanetics.agent.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigSource.class);
  private static final Properties properties = new Properties();

  private final String fileName;

  public FileConfigSource(String fileName) {
    this.fileName = fileName;
    loadPropertiesFromFile();
  }

  @Override
  public Optional<String> getProperty(String key) {
    return Optional.ofNullable(properties.getProperty(key));
  }

  private void loadPropertiesFromFile() {
    try (InputStream input =
        FileConfigSource.class.getClassLoader().getResourceAsStream(fileName)) {
      if (input != null) {
        properties.load(input);
        LOGGER.info("Loaded configuration from {}.", fileName);
      } else {
        LOGGER.warn("Could not find {}. Ignoring this source.", fileName);
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to load {}. Ignoring this source. {}", fileName, e.getMessage());
    }
  }
}
