package com.aqanetics.agent.core.dto;

import java.time.Instant;

public record TestExecutionLogDto(String message, String level, Instant timestamp) {}
