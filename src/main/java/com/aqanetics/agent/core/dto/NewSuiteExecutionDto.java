package com.aqanetics.agent.core.dto;

import com.aqanetics.dto.basic.BasicSuiteExecutionParameterDto;
import com.aqanetics.dto.normal.OrganizationDto;
import com.aqanetics.enums.ExecutionStatus;
import java.time.Instant;
import java.util.List;

public record NewSuiteExecutionDto(
    Instant startTime,
    List<BasicSuiteExecutionParameterDto> parameters,
    OrganizationDto organization,
    String hostName,
    ExecutionStatus status) {}
