package com.aqanetics.agent.testng.dto;

import java.time.Instant;

public record MinimalTestExecutionDto(Long id, String testName, Instant startTime, Long retryOf) {

}
