package com.aqanetics.agent.testng;

import com.aqanetics.dto.minimal.MinimalMethodExecutionDto;

public class ExecutionEntities {

  /** Id of current suite execution. */
  public static volatile Long suiteExecutionId;

  /** Entity of currently in progress @Test. */
  public static volatile MinimalMethodExecutionDto testExecution;

  /** Entity of finished @Test. */
  public static volatile MinimalMethodExecutionDto prevTestExecution;

  /** Entity of currently in progress @Before or @After */
  public static volatile MinimalMethodExecutionDto configurationExecution;

  /** Id of current in progress method. */
  public static volatile Long inProgressMethodExecutionId;
}
