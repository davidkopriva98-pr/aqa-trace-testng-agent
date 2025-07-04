package com.aqanetics.agent.testng;

import com.aqanetics.dto.minimal.MinimalTestExecutionDto;

public class ExecutionEntities {

  public static volatile Long suiteExecutionId;

  public static volatile MinimalTestExecutionDto testExecution;
  public static volatile MinimalTestExecutionDto prevTestExecution;
  public static volatile MinimalTestExecutionDto configurationExecution;
  public static volatile Long inProgressTestExecutionId;
}
