package com.aqanetics.agent.testng.listener;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.METHOD_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.core.exception.AqaAgentException;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.agent.utils.AqaTraceServerGuards;
import com.aqanetics.agent.utils.CrudMethods;
import com.aqanetics.agent.utils.MethodExecutionListenerUtils;
import com.aqanetics.dto.create.NewMethodExecutionDto;
import com.aqanetics.dto.minimal.MinimalMethodExecutionDto;
import com.aqanetics.enums.ExecutionStatus;
import com.aqanetics.enums.MethodExecutionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class MethodExecutionListener implements ITestListener, IConfigurationListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodExecutionListener.class);
  private static final boolean STOP_WHEN_UNREACHABLE =
      AqaConfigLoader.getBooleanProperty("aqa-trace.stop-execution-when-unreachable", false);

  public MethodExecutionListener() {}

  private void startTestExecution(
      MinimalMethodExecutionDto methodExecution, Map<String, Object> values) {
    if ((!AqaTraceServerGuards.isSuiteExecIdSet() || AqaTraceServerGuards.isServerUnreachable())
        && STOP_WHEN_UNREACHABLE) {
      return;
    }
    try {
      String response =
          CrudMethods.sendPost(
              URI.create(
                  AqaConfigLoader.API_ENDPOINT
                      + AGENT_API_ENDPOINT
                      + SUITE_API_ENDPOINT
                      + ExecutionEntities.suiteExecutionId
                      + METHOD_API_ENDPOINT
                      + methodExecution.id()
                      + "/start"),
              AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(values));
      if (response != null) {
        LOGGER.debug("methodExecution {} started.", methodExecution.id());
        ExecutionEntities.inProgressMethodExecutionId = methodExecution.id();
      }
    } catch (AqaAgentException aqaException) {
      if (aqaException.shouldThrowException()) {
        LOGGER.error("Error updating methodExecution: {}", aqaException.getMessage());
        throw new RuntimeException(aqaException);
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while JSON parsing suiteExecution: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Test passed.");
      MethodExecutionListenerUtils.testExecutionEnded(result);
    }
  }

  @Override
  public void onTestFailedWithTimeout(ITestResult result) {
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Test failed with timeout.");
      MethodExecutionListenerUtils.testExecutionEnded(result);
    }
  }

  @Override
  public void onTestFailure(ITestResult result) {
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Test failed.");
      MethodExecutionListenerUtils.testExecutionEnded(result);
    }
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Test skipped.");
      MethodExecutionListenerUtils.testExecutionEnded(result);
    }
  }

  @Override
  public void beforeConfiguration(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.beforeConfiguration(tr, tm);

    if (AqaTraceServerGuards.isEnabledExtended()
        && MethodExecutionListenerUtils.notMarkedAsIgnored(tr)) {
      ITestNGMethod method = tr.getMethod();
      LOGGER.debug("beforeConfiguration: {}", method.getMethodName());
      String sessionId = MethodExecutionListenerUtils.getSessionId(tr.getTestContext());

      if (method.isBeforeMethodConfiguration() && tm != null) {
        // tm is needed to get @Test name, otherwise we cannot create test entity in the DB
        MethodExecutionListenerUtils.handleBeforeMethodConfiguration(tr, tm, sessionId);
      } else if (method.isAfterMethodConfiguration()) {
        MethodExecutionListenerUtils.handleAfterMethodConfiguration(tr, sessionId);
      } else if (method.isBeforeClassConfiguration()) {
        MethodExecutionListenerUtils.handleConfiguration(
            tr, MethodExecutionType.BEFORE_CLASS, sessionId);
      } else if (method.isAfterClassConfiguration()) {
        MethodExecutionListenerUtils.handleConfiguration(
            tr, MethodExecutionType.AFTER_CLASS, sessionId);
      } else if (method.isBeforeTestConfiguration()) {
        MethodExecutionListenerUtils.handleConfiguration(
            tr, MethodExecutionType.BEFORE_TEST, sessionId);
      } else if (method.isAfterTestConfiguration()) {
        MethodExecutionListenerUtils.handleConfiguration(
            tr, MethodExecutionType.AFTER_TEST, sessionId);
      }
    }
  }

  @Override
  public void onConfigurationSuccess(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.onConfigurationSuccess(tr, tm);
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Config method passed.");
      MethodExecutionListenerUtils.configurationExecutionEnded(tr);
    }
  }

  @Override
  public void onConfigurationFailure(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.onConfigurationFailure(tr, tm);
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Config method failed.");
      MethodExecutionListenerUtils.configurationExecutionEnded(tr);
    }
  }

  @Override
  public void onConfigurationSkip(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.onConfigurationSkip(tr, tm);
    if (AqaTraceServerGuards.isEnabledExtended()) {
      LOGGER.debug("Config method skipped.");
      MethodExecutionListenerUtils.configurationExecutionEnded(tr);
    }
  }

  @Override
  public void onTestStart(ITestResult result) {
    ITestListener.super.onTestStart(result);

    if (AqaTraceServerGuards.isEnabledExtended()
        && MethodExecutionListenerUtils.notMarkedAsIgnored(result)) {
      /*For non-test methods see TestExecutionListener.beforeConfiguration() */

      int retryCount = result.getMethod().getCurrentInvocationCount();

      // Check if testExecution entity was already created (happens when @Test has @Before)
      if (ExecutionEntities.testExecution != null
          && ExecutionEntities.testExecution.name().equals(result.getMethod().getMethodName())
          && ExecutionEntities.testExecution.startTime() == null) {
        ExecutionEntities.inProgressMethodExecutionId = ExecutionEntities.testExecution.id();

        Map<String, Object> values =
            new HashMap<>(
                Map.ofEntries(
                    Map.entry("status", ExecutionStatus.IN_PROGRESS),
                    Map.entry(
                        "sessionId",
                        Objects.requireNonNullElse(
                            MethodExecutionListenerUtils.getSessionId(result.getTestContext()),
                            "")),
                    Map.entry("startTime", Instant.now().toString())));
        if (retryCount > 0) {
          values.put("retryCount", retryCount);
        }

        this.startTestExecution(ExecutionEntities.testExecution, values);
        LOGGER.debug("Started test execution {}", ExecutionEntities.testExecution.id());

      } else {

        Long retryOf = null;
        ExecutionEntities.prevTestExecution = ExecutionEntities.testExecution;

        if (ExecutionEntities.prevTestExecution != null
            && ExecutionEntities.prevTestExecution.name().equals(result.getMethod().getMethodName())
            && retryCount > 0) {
          LOGGER.debug(
              "Previous test execution: [id {}, name {}]. New: {}",
              ExecutionEntities.prevTestExecution.id(),
              ExecutionEntities.prevTestExecution.name(),
              result.getMethod().getMethodName());
          retryOf = ExecutionEntities.prevTestExecution.id();
        }

        ExecutionEntities.testExecution =
            MethodExecutionListenerUtils.registerNewTestExecution(
                new NewMethodExecutionDto(
                    result.getMethod().getMethodName(),
                    result.getMethod().getTestClass().getRealClass().getSimpleName(),
                    MethodExecutionType.TEST,
                    ExecutionStatus.IN_PROGRESS,
                    result.getMethod().getTestClass().getName(),
                    Instant.ofEpochMilli(result.getMethod().getDate()),
                    Instant.ofEpochMilli(result.getMethod().getDate()),
                    result.getMethod().getCurrentInvocationCount(),
                    retryOf,
                    null,
                    Objects.requireNonNullElse(
                        MethodExecutionListenerUtils.getSessionId(result.getTestContext()), "")));
        ExecutionEntities.inProgressMethodExecutionId = ExecutionEntities.testExecution.id();
        LOGGER.debug(
            "@Test {} had no configurations. Created new test execution",
            ExecutionEntities.testExecution.id());
      }
    }
  }
}
