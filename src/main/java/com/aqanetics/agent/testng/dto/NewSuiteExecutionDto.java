package com.aqanetics.agent.testng.dto;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record NewSuiteExecutionDto(Instant startTime, Boolean inProgress,
                                   List<ParameterDto> parameters, OrganizationDto organization,
                                   String hostName) {

  @Override
  public String toString() {
    return String.format("NewSuiteExecution{%s, %s, [%d, %s]}", startTime.toString(), inProgress,
        parameters.size(), parameters.stream().map(
            ParameterDto::type).collect(Collectors.joining(",")));
  }
}
