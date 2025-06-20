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
    Long partOf,
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
      String sessionId,
      Long retryOf) {
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
        retryOf,
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
      String sessionId,
      Long partOf) {
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
        partOf,
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
        null,
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
