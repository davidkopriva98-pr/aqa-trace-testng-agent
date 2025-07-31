package com.aqanetics.agent.utils;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.testng.ExecutionEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AqaTraceServerGuards {

  private static final Logger LOGGER = LoggerFactory.getLogger(AqaTraceServerGuards.class);

  private static volatile boolean serverUnreachable = false;

  public static void markServerUnreachable() {
    LOGGER.debug("Server unreachable");
    serverUnreachable = true;
  }

  public static boolean isEnabledExtended() {
    return AqaConfigLoader.ENABLED && AqaConfigLoader.API_ENDPOINT != null && !serverUnreachable;
  }

  public static boolean isEnabled() {
    return AqaConfigLoader.ENABLED && AqaConfigLoader.API_ENDPOINT != null;
  }

  public static boolean isServerUnreachable() {
    return serverUnreachable;
  }

  public static boolean isSuiteExecIdSet() {
    return ExecutionEntities.suiteExecutionId != null;
  }
}
