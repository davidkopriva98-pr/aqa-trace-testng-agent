package com.aqanetics.agent.core.dto;

import java.time.Instant;

public record NewTestExecutionDto(
    String testName,
    String testClassName,
    String type,
    String status,
    String testClasspath,
    Instant createdTime,
    Boolean inProgress,
    Instant startTime,
    Integer retryCount,
    Long retryOf,
    String sessionId) {

  public NewTestExecutionDto(
      String testName,
      String testClassName,
      String type,
      String status,
      String testClasspath,
      Instant createdTime,
      Boolean inProgress,
      String sessionId) {
    this(
        testName,
        testClassName,
        type,
        status,
        testClasspath,
        createdTime,
        inProgress,
        null,
        null,
        null,
        sessionId);
  }

  public NewTestExecutionDto(
      String testName,
      String testClassName,
      String type,
      String testClasspath,
      Instant createdTime,
      Boolean inProgress,
      Instant startTime,
      String sessionId) {
    this(
        testName,
        testClassName,
        type,
        null,
        testClasspath,
        createdTime,
        inProgress,
        startTime,
        null,
        null,
        sessionId);
  }

  public NewTestExecutionDto(
      String testName,
      String testClassName,
      String type,
      String testClasspath,
      Instant createdTime,
      Boolean inProgress,
      Instant startTime,
      Integer retryCount,
      Long retryOf,
      String sessionId) {
    this(
        testName,
        testClassName,
        type,
        null,
        testClasspath,
        createdTime,
        inProgress,
        startTime,
        retryCount,
        retryOf,
        sessionId);
  }

  @Override
  public String toString() {
    return String.format(
        "%s %s %s %s, retry of: %d",
        this.testName, this.testClassName, this.type, this.createdTime, this.retryOf);
  }
}
