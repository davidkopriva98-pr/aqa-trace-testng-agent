package com.aqanetics.agent.testng;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.ARTIFACTS_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.TEST_API_ENDPOINT;
import static com.aqanetics.agent.utils.CrudMethods.postExecutionArtifact;

import com.aqanetics.agent.config.AqaConfigLoader;
import java.io.File;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactsHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsHelper.class);
  private static final boolean ENABLED_ARTIFACTS =
      Objects.equals(AqaConfigLoader.getProperty("aqa-trace.artifacts.enabled", "false"), "true");
  private static final boolean ENABLED_TEST_SCREENSHOT =
      Objects.equals(
          AqaConfigLoader.getProperty("aqa-trace.artifacts.test-error-screenshot", "false"),
          "true");
  private static final boolean ENABLED_CONFIGURATION_SCREENSHOT =
      Objects.equals(
          AqaConfigLoader.getProperty(
              "aqa-trace.artifacts.configuration-error-screenshot", "false"),
          "true");

  public static void uploadConfigurationArtifact(File artifact) {
    uploadFile(
        artifact,
        ExecutionEntities.currentNotTestExecution.id(),
        ExecutionEntities.currentNotTestExecution.testName(),
        false);
  }

  public static void uploadTestArtifact(File artifact) {
    uploadFile(
        artifact,
        ExecutionEntities.testExecution.id(),
        ExecutionEntities.testExecution.testName(),
        false);
  }

  public static void uploadTestErrorScreenshot(File artifact) {
    if (ENABLED_TEST_SCREENSHOT) {
      uploadFile(
          artifact,
          ExecutionEntities.testExecution.id(),
          ExecutionEntities.testExecution.testName(),
          true);
    }
  }

  public static void uploadConfigurationErrorScreenshot(File artifact) {
    if (ENABLED_CONFIGURATION_SCREENSHOT) {
      uploadFile(
          artifact,
          ExecutionEntities.currentNotTestExecution.id(),
          ExecutionEntities.currentNotTestExecution.testName(),
          true);
    }
  }

  private static void uploadFile(
      File artifact, Long executionId, String executionName, boolean errorScreenshot) {
    if (ENABLED_ARTIFACTS) {
      String url =
          API_ENDPOINT
              + AGENT_API_ENDPOINT
              + SUITE_API_ENDPOINT
              + ExecutionEntities.suiteExecutionId
              + TEST_API_ENDPOINT
              + executionId
              + ARTIFACTS_ENDPOINT
              + (errorScreenshot ? "/error-screenshot" : "");
      LOGGER.info(
          "Uploading {} {} to test execution {}",
          errorScreenshot ? "error screenshot" : "artifact",
          artifact.getName(),
          executionName);
      postExecutionArtifact(url, artifact, artifact.getName(), true);
    }
  }
}
