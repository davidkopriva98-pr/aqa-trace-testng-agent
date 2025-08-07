package com.aqanetics.agent.utils;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.TEST_API_ENDPOINT;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.core.exception.AqaAgentException;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.dto.create.NewTestExecutionDto;
import com.aqanetics.dto.minimal.MinimalTestExecutionDto;
import com.aqanetics.dto.normal.TestExecutionLogDto;
import com.aqanetics.enums.ExecutionStatus;
import com.aqanetics.enums.MethodExecutionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class MethodExecutionListenerUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodExecutionListenerUtils.class);

  private static final boolean STOP_WHEN_UNREACHABLE =
      AqaConfigLoader.getBooleanProperty("aqa-trace.stop-execution-when-unreachable", false);

  public static MinimalTestExecutionDto registerNewTestExecution(
      NewTestExecutionDto newTestExecution) {
    if ((!AqaTraceServerGuards.isSuiteExecIdSet() || AqaTraceServerGuards.isServerUnreachable())
        && STOP_WHEN_UNREACHABLE) {
      return null;
    }

    try {
      String response =
          CrudMethods.sendPost(
              URI.create(
                  AqaConfigLoader.API_ENDPOINT
                      + AGENT_API_ENDPOINT
                      + SUITE_API_ENDPOINT
                      + ExecutionEntities.suiteExecutionId
                      + TEST_API_ENDPOINT
                      + "new"),
              AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(newTestExecution));
      if (response != null) {
        LOGGER.debug("Received testExecution");
        return AqaConfigLoader.OBJECT_MAPPER.readValue(response, MinimalTestExecutionDto.class);
      } else {
        AqaTraceServerGuards.markServerUnreachable();
        return null;
      }
    } catch (AqaAgentException aqaException) {
      if (aqaException.shouldThrowException()) {
        LOGGER.error("Error creating new testExecution: {}", aqaException.getMessage());
        throw new RuntimeException(aqaException);
      }
      return null;
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while JSON parsing suiteExecution: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public static void configurationExecutionEnded(ITestResult tr) {
    if (notMarkedAsIgnored(tr) && ExecutionEntities.configurationExecution != null) {
      Map<String, Object> values = MethodExecutionListenerUtils.prepareTestEndParameters(tr);
      MethodExecutionListenerUtils.endTestExecution(
          ExecutionEntities.configurationExecution.id(), values);
      ExecutionEntities.configurationExecution = null;
    }
  }

  public static boolean notMarkedAsIgnored(ITestResult tr) {
    AQATraceIgnore ignore =
        tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(AQATraceIgnore.class);
    return ignore == null || !ignore.value();
  }

  public static String getSessionId(ITestContext context) {
    if (context.getAttribute("sessionId") != null) {
      return context.getAttribute("sessionId").toString();
    }
    return null;
  }

  private static ExecutionStatus convertIStatusToString(int status) {
    return switch (status) {
      case 1 -> ExecutionStatus.PASSED;
      case 2 -> ExecutionStatus.FAILED;
      case 3 -> ExecutionStatus.SKIPPED;
      default -> ExecutionStatus.UNKNOWN;
    };
  }

  public static void testExecutionEnded(ITestResult result) {
    AQATraceIgnore ignore =
        result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(AQATraceIgnore.class);
    if (ignore == null || !ignore.value()) {
      Map<String, Object> values = MethodExecutionListenerUtils.prepareTestEndParameters(result);
      Long testId = ExecutionEntities.testExecution.id();
      ExecutionEntities.testExecution =
          MethodExecutionListenerUtils.endTestExecution(testId, values);
    }
  }

  private static String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    return sw.toString();
  }

  public static Map<String, Object> prepareTestEndParameters(ITestResult tr) {
    Map<String, Object> values =
        new HashMap<>(
            Map.of(
                "status",
                convertIStatusToString(tr.getStatus()),
                "endTime",
                Instant.now().toString()));

    LOGGER.debug("{} {}", tr.getStartMillis(), tr.getEndMillis());

    // Remove tr.getStartMillis() != tr.getEndMillis() from if condition, if we want to have error
    // from
    // before method listed for skipped test
    if (tr.getStartMillis() != tr.getEndMillis()
        && tr.getThrowable() != null
        && tr.getThrowable().getMessage() != null) {
      String stackTrace = getStackTrace(tr.getThrowable());
      values.put("exceptionStackTrace", stackTrace);
      values.put("exceptionMessage", tr.getThrowable().getMessage());

      // Only post log if in progress execution id exists.
      if (ExecutionEntities.inProgressTestExecutionId != null) {
        try {
          CrudMethods.postLog(
              AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(
                  new TestExecutionLogDto(
                      ExecutionEntities.inProgressTestExecutionId,
                      stackTrace,
                      "ERROR",
                      Instant.ofEpochMilli(tr.getEndMillis()))));
        } catch (Exception e) {
          LOGGER.error("Error posting error log: {}", e.getMessage());
        }
      }
    }
    return values;
  }

  public static MinimalTestExecutionDto endTestExecution(
      Long testExecutionId, Map<String, Object> values) {
    if ((!AqaTraceServerGuards.isSuiteExecIdSet() || AqaTraceServerGuards.isServerUnreachable())
        && STOP_WHEN_UNREACHABLE) {
      return null;
    }
    try {
      String response =
          CrudMethods.sendPost(
              URI.create(
                  AqaConfigLoader.API_ENDPOINT
                      + AGENT_API_ENDPOINT
                      + SUITE_API_ENDPOINT
                      + ExecutionEntities.suiteExecutionId
                      + TEST_API_ENDPOINT
                      + testExecutionId
                      + "/end"),
              AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(values));
      if (response != null) {
        LOGGER.debug("testExecution {} ended.", testExecutionId);
        ExecutionEntities.inProgressTestExecutionId = null;
        return AqaConfigLoader.OBJECT_MAPPER.readValue(response, MinimalTestExecutionDto.class);
      } else {
        return null;
      }
    } catch (AqaAgentException aqaException) {
      if (aqaException.shouldThrowException()) {
        LOGGER.error("Error stopping testExecution: {}", aqaException.getMessage());
        throw new RuntimeException(aqaException);
      }
      return null;
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while JSON parsing suiteExecution: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public static void handleBeforeMethodConfiguration(
      ITestResult tr, ITestNGMethod tm, String sessionId) {
    LOGGER.debug("Before config method starting");
    // Configuration method has a fk link to @Test, therefore we need to check if test is created in
    // DB.
    if (ExecutionEntities.testExecution == null
        || !Objects.equals(ExecutionEntities.testExecution.testName(), tm.getMethodName())
        || (tr.getMethod().isBeforeMethodConfiguration()
            && ExecutionEntities.testExecution.endTime() != null)) {
      // Checking if @Test is already created in DB. If not, we create it and set it as SKIPPED
      // in case configuration method fails.
      ExecutionEntities.prevTestExecution = ExecutionEntities.testExecution;

      ExecutionEntities.testExecution =
          MethodExecutionListenerUtils.registerNewTestExecution(
              new NewTestExecutionDto(
                  tm.getMethodName(),
                  tm.getTestClass().getRealClass().getSimpleName(),
                  MethodExecutionType.TEST,
                  ExecutionStatus.SKIPPED,
                  tr.getTestClass().getName(),
                  Instant.now(),
                  null,
                  null,
                  ExecutionEntities.prevTestExecution != null
                      ? ExecutionEntities.prevTestExecution.id()
                      : null,
                  null,
                  sessionId));
      if (ExecutionEntities.testExecution != null) {
        LOGGER.debug(
            "Test execution {} was missing for current configuration method",
            ExecutionEntities.testExecution.id());
      }
    }
    handleMethodConfiguration(tr, MethodExecutionType.BEFORE_METHOD, sessionId);
  }

  public static void handleAfterMethodConfiguration(ITestResult tr, String sessionId) {
    LOGGER.debug("After config method starting");
    handleMethodConfiguration(tr, MethodExecutionType.AFTER_METHOD, sessionId);
  }

  public static void handleConfiguration(
      ITestResult tr, MethodExecutionType type, String sessionId) {
    LOGGER.debug("Config class starting");
    registerNewMethodExecution(tr, type, null, sessionId);
  }

  private static void registerNewMethodExecution(
      ITestResult tr, MethodExecutionType type, Long testExecutionId, String sessionId) {
    NewTestExecutionDto newConfigurationExecution =
        new NewTestExecutionDto(
            tr.getMethod().getMethodName(),
            tr.getTestClass().getRealClass().getSimpleName(),
            type,
            ExecutionStatus.IN_PROGRESS,
            tr.getTestClass().getName(),
            Instant.now(),
            Instant.now(),
            null,
            null,
            testExecutionId,
            sessionId);

    ExecutionEntities.configurationExecution =
        MethodExecutionListenerUtils.registerNewTestExecution(newConfigurationExecution);
    ExecutionEntities.inProgressTestExecutionId = ExecutionEntities.configurationExecution.id();
    LOGGER.debug(
        "Configuration method ({}) started", ExecutionEntities.configurationExecution.id());
  }

  private static void handleMethodConfiguration(
      ITestResult tr, MethodExecutionType type, String sessionId) {
    if (ExecutionEntities.testExecution != null) {
      registerNewMethodExecution(tr, type, ExecutionEntities.testExecution.id(), sessionId);
    }
  }
}
