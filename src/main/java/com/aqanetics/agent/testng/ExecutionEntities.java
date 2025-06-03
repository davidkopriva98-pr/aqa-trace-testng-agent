package com.aqanetics.agent.testng;

import com.aqanetics.agent.testng.dto.MinimalTestExecutionDto;

public class ExecutionEntities {

  public static volatile Long suiteExecutionId;

  public static volatile MinimalTestExecutionDto testExecution;
  public static volatile MinimalTestExecutionDto currentNotTestExecution;
  public static volatile Long inProgressTestExecutionId;
}
