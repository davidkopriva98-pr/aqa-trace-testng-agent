package com.aqanetics.agent.testng.dto;

import java.time.Instant;

public record NewTestExecutionDto(String testName,
                                  String testClassName,
                                  String type,
                                  String status,
                                  String testClasspath,
                                  Instant createdTime,
                                  Boolean inProgress,
                                  Instant startTime,
                                  Integer retryCount,
                                  Long retryOf) {

  public NewTestExecutionDto(String testName, String testClassName, String type, String status,
      String testClasspath, Instant createdTime, Boolean inProgress) {
    this(testName, testClassName, type, status, testClasspath, createdTime, inProgress, null,
        null, null);
  }

  public NewTestExecutionDto(String testName, String testClassName, String type,
      String testClasspath,
      Instant createdTime, Boolean inProgress, Instant startTime) {
    this(testName, testClassName, type, null, testClasspath, createdTime, inProgress,
        startTime, null, null);
  }

  public NewTestExecutionDto(String testName, String testClassName, String type,
      String testClasspath,
      Instant createdTime, Boolean inProgress, Instant startTime, Integer retryCount,
      Long retryOf) {
    this(testName, testClassName, type, null, testClasspath, createdTime, inProgress,
        startTime, retryCount, retryOf);
  }

  @Override
  public String toString() {
    return String.format("%s %s %s %s, retry of: %d", this.testName, this.testClassName, this.type,
        this.createdTime, this.retryOf);
  }
}
