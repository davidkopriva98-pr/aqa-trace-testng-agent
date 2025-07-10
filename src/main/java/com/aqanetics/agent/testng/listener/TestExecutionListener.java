package com.aqanetics.agent.testng.listener;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.TEST_API_ENDPOINT;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.core.exception.AqaAgentException;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.agent.utils.AQATraceIgnore;
import com.aqanetics.agent.utils.CrudMethods;
import com.aqanetics.dto.create.NewTestExecutionDto;
import com.aqanetics.dto.minimal.MinimalTestExecutionDto;
import com.aqanetics.dto.normal.TestExecutionLogDto;
import com.aqanetics.enums.ExecutionStatus;
import com.aqanetics.enums.TestExecutionType;
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
import org.testng.IConfigurationListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class TestExecutionListener
    implements ITestListener, IInvokedMethodListener, IConfigurationListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestExecutionListener.class);
  private static final boolean STOP_WHEN_UNREACHABLE =
      AqaConfigLoader.getBooleanProperty("aqa-trace.stop-execution-when-unreachable", false);

  public TestExecutionListener() {}

  private MinimalTestExecutionDto registerNewTestExecution(NewTestExecutionDto newTestExecution) {
    if (ExecutionEntities.suiteExecutionId == null && STOP_WHEN_UNREACHABLE) {
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

  private void startTestExecution(
      MinimalTestExecutionDto testExecution, Map<String, Object> values) {
    if (ExecutionEntities.suiteExecutionId == null && STOP_WHEN_UNREACHABLE) {
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
                      + TEST_API_ENDPOINT
                      + testExecution.id()
                      + "/start"),
              AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(values));
      if (response != null) {
        LOGGER.info("testExecution {} started.", testExecution.id());
        ExecutionEntities.inProgressTestExecutionId = testExecution.id();
      }
    } catch (AqaAgentException aqaException) {
      if (aqaException.shouldThrowException()) {
        LOGGER.error("Error updating testExecution: {}", aqaException.getMessage());
        throw new RuntimeException(aqaException);
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while JSON parsing suiteExecution: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private MinimalTestExecutionDto endTestExecution(
      Long testExecutionId, Map<String, Object> values) {
    if (ExecutionEntities.suiteExecutionId == null && STOP_WHEN_UNREACHABLE) {
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
        LOGGER.info("testExecution {} ended.", testExecutionId);
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

  @Override
  public void onTestSuccess(ITestResult result) {
    this.testEnded(result);
  }

  @Override
  public void onTestFailedWithTimeout(ITestResult result) {
    this.testEnded(result);
  }

  @Override
  public void onTestFailure(ITestResult result) {
    this.testEnded(result);
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    this.testEnded(result);
  }

  private String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    return sw.toString();
  }

  private void testEnded(ITestResult result) {
    AQATraceIgnore ignore =
        result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(AQATraceIgnore.class);
    if (ignore == null || !ignore.value()) {
      Map<String, Object> values = prepareTestEndParameters(result);
      Long testId = ExecutionEntities.testExecution.id();
      ExecutionEntities.testExecution = this.endTestExecution(testId, values);
    }
  }

  @Override
  public void beforeConfiguration(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.beforeConfiguration(tr, tm);
    if (tm != null) {
      AQATraceIgnore ignore =
          tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(AQATraceIgnore.class);
      if ((ignore == null || !ignore.value())) {
        String sessionId = tr.getTestContext().getAttribute("sessionId").toString();

        LOGGER.debug("Config method starting");
        if (ExecutionEntities.testExecution == null
            || !Objects.equals(ExecutionEntities.testExecution.testName(), tm.getMethodName())
            || (tr.getMethod().isBeforeMethodConfiguration()
                && ExecutionEntities.testExecution.endTime() != null)) {
          // Checking if @Test is already created in DB. If not, we create it and set it as SKIPPED
          // in case configuration method fails.
          ExecutionEntities.prevTestExecution = ExecutionEntities.testExecution;

          ExecutionEntities.testExecution =
              this.registerNewTestExecution(
                  new NewTestExecutionDto(
                      tm.getMethodName(),
                      tm.getTestClass().getRealClass().getSimpleName(),
                      TestExecutionType.TEST,
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
            LOGGER.info(
                "Test execution {} was missing for current configuration method",
                ExecutionEntities.testExecution.id());
          }
        }

        if (ExecutionEntities.testExecution != null) {

          NewTestExecutionDto newConfigurationExecution =
              new NewTestExecutionDto(
                  tr.getMethod().getMethodName(),
                  tm.getTestClass().getRealClass().getSimpleName(),
                  tr.getMethod().isBeforeMethodConfiguration()
                      ? TestExecutionType.BEFORE_METHOD
                      : TestExecutionType.AFTER_METHOD,
                  ExecutionStatus.IN_PROGRESS,
                  tr.getTestClass().getName(),
                  Instant.now(),
                  Instant.now(),
                  null,
                  null,
                  ExecutionEntities.testExecution.id(),
                  sessionId);

          ExecutionEntities.configurationExecution =
              this.registerNewTestExecution(newConfigurationExecution);
          ExecutionEntities.inProgressTestExecutionId =
              ExecutionEntities.configurationExecution.id();
          LOGGER.info(
              "New configuration method started: {}",
              ExecutionEntities.configurationExecution.id());
        }
      }
    }
  }

  @Override
  public void onConfigurationSuccess(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.onConfigurationSuccess(tr, tm);
    this.methodConfigurationEnd(tr, tm);
  }

  @Override
  public void onConfigurationFailure(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.onConfigurationFailure(tr, tm);
    this.methodConfigurationEnd(tr, tm);
  }

  @Override
  public void onConfigurationSkip(ITestResult tr, ITestNGMethod tm) {
    IConfigurationListener.super.onConfigurationSkip(tr, tm);
    this.methodConfigurationEnd(tr, tm);
  }

  private void methodConfigurationEnd(ITestResult tr, ITestNGMethod tm) {
    if (tm != null) {
      AQATraceIgnore ignore = tm.getClass().getAnnotation(AQATraceIgnore.class);
      if ((ignore == null || !ignore.value()) && ExecutionEntities.configurationExecution != null) {
        Map<String, Object> values = prepareTestEndParameters(tr);
        this.endTestExecution(ExecutionEntities.configurationExecution.id(), values);
        ExecutionEntities.configurationExecution = null;
      }
    }
  }

  private Map<String, Object> prepareTestEndParameters(ITestResult tr) {
    Map<String, Object> values =
        new HashMap<>(
            Map.of(
                "status",
                this.convertIStatusToString(tr.getStatus()),
                "endTime",
                Instant.now().toString()));
    if (tr.getThrowable() != null && tr.getThrowable().getMessage() != null) {
      String stackTrace = getStackTrace(tr.getThrowable());
      values.put("exceptionStackTrace", stackTrace);
      values.put("exceptionMessage", tr.getThrowable().getMessage());

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
    return values;
  }

  private ExecutionStatus convertIStatusToString(int status) {
    return switch (status) {
      case 1 -> ExecutionStatus.PASSED;
      case 2 -> ExecutionStatus.FAILED;
      case 3 -> ExecutionStatus.SKIPPED;
      default -> ExecutionStatus.UNKNOWN;
    };
  }

  @Override
  public void beforeInvocation(
      IInvokedMethod method, ITestResult testResult, ITestContext context) {
    IInvokedMethodListener.super.beforeInvocation(method, testResult, context);

    /*For non-test methods see TestExecutionListener.beforeConfiguration() */
    AQATraceIgnore ignore =
        method
            .getTestMethod()
            .getConstructorOrMethod()
            .getMethod()
            .getAnnotation(AQATraceIgnore.class);
    if (method.isTestMethod() && (ignore == null || !ignore.value())) {
      int retryCount = method.getTestMethod().getCurrentInvocationCount();

      // Check if testExecution entity was already created (happens when @Test has @Before)
      if (ExecutionEntities.testExecution != null
          && ExecutionEntities.testExecution
              .testName()
              .equals(method.getTestMethod().getMethodName())
          && ExecutionEntities.testExecution.startTime() == null) {
        ExecutionEntities.inProgressTestExecutionId = ExecutionEntities.testExecution.id();

        Map<String, Object> values =
            new HashMap<>(
                Map.ofEntries(
                    Map.entry("status", ExecutionStatus.IN_PROGRESS),
                    Map.entry("sessionId", context.getAttribute("sessionId")),
                    Map.entry("startTime", Instant.now().toString())));
        if (retryCount > 0) {
          values.put("retryCount", retryCount);
        }

        this.startTestExecution(ExecutionEntities.testExecution, values);
        LOGGER.info("Started test execution {}", ExecutionEntities.testExecution.id());

      } else {

        Long retryOf = null;
        ExecutionEntities.prevTestExecution = ExecutionEntities.testExecution;

        if (ExecutionEntities.prevTestExecution != null
            && ExecutionEntities.prevTestExecution
                .testName()
                .equals(method.getTestMethod().getMethodName())
            && retryCount > 0) {
          LOGGER.info(
              "Previous test execution: [id {}, name {}]. New: {}",
              ExecutionEntities.prevTestExecution.id(),
              ExecutionEntities.prevTestExecution.testName(),
              method.getTestMethod().getMethodName());
          retryOf = ExecutionEntities.prevTestExecution.id();
        }

        ExecutionEntities.testExecution =
            this.registerNewTestExecution(
                new NewTestExecutionDto(
                    method.getTestMethod().getMethodName(),
                    method.getTestMethod().getTestClass().getRealClass().getSimpleName(),
                    TestExecutionType.TEST,
                    ExecutionStatus.IN_PROGRESS,
                    method.getTestMethod().getTestClass().getName(),
                    Instant.ofEpochMilli(method.getDate()),
                    Instant.ofEpochMilli(method.getDate()),
                    method.getTestMethod().getCurrentInvocationCount(),
                    retryOf,
                    null,
                    context.getAttribute("sessionId").toString()));
        ExecutionEntities.inProgressTestExecutionId = ExecutionEntities.testExecution.id();
        LOGGER.info(
            "@Test {} had no configurations. Created new test execution",
            ExecutionEntities.testExecution.id());
      }
    }
  }
}
