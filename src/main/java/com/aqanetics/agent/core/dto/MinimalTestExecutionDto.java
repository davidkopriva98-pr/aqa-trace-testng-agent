package com.aqanetics.agent.core.dto;

import java.time.Instant;

public record MinimalTestExecutionDto(
    Long id, String testName, Instant startTime, Long retryOf, String sessionId) {}
