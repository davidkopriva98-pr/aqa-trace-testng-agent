package com.aqanetics.agent.testng.dto;

import java.time.Instant;

public record TestExecutionLogDto(String message, String level, Instant timestamp) {}
