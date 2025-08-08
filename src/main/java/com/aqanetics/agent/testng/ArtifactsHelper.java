package com.aqanetics.agent.testng;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.ARTIFACTS_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.METHOD_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.agent.utils.CrudMethods.postExecutionArtifact;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.core.exception.AqaAgentException;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactsHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsHelper.class);
  private static final boolean ENABLED_ARTIFACTS =
      AqaConfigLoader.getBooleanProperty("aqa-trace.artifacts.enabled", false);
  private static final boolean ENABLED_TEST_SCREENSHOT =
      AqaConfigLoader.getBooleanProperty(
          "aqa-trace.artifacts.test-error-screenshot-enabled", false);
  private static final boolean ENABLED_CONFIGURATION_SCREENSHOT =
      AqaConfigLoader.getBooleanProperty(
          "aqa-trace.artifacts.configuration-error-screenshot-enabled", false);

  public static void uploadConfigurationArtifact(File artifact) {
    uploadFile(
        artifact,
        ExecutionEntities.configurationExecution.id(),
        ExecutionEntities.configurationExecution.name(),
        false);
  }

  public static void uploadTestArtifact(File artifact) {
    uploadFile(
        artifact,
        ExecutionEntities.testExecution.id(),
        ExecutionEntities.testExecution.name(),
        false);
  }

  public static void uploadTestErrorScreenshot(File artifact) {
    if (ENABLED_TEST_SCREENSHOT) {
      uploadFile(
          artifact,
          ExecutionEntities.testExecution.id(),
          ExecutionEntities.testExecution.name(),
          true);
    }
  }

  public static void uploadConfigurationErrorScreenshot(File artifact) {
    if (ENABLED_CONFIGURATION_SCREENSHOT) {
      uploadFile(
          artifact,
          ExecutionEntities.configurationExecution.id(),
          ExecutionEntities.configurationExecution.name(),
          true);
    }
  }

  private static void uploadFile(
      File artifact, Long methodExecutionId, String executionName, boolean errorScreenshot) {
    if (ENABLED_ARTIFACTS && methodExecutionId != null) {
      String url =
          API_ENDPOINT
              + AGENT_API_ENDPOINT
              + SUITE_API_ENDPOINT
              + ExecutionEntities.suiteExecutionId
              + METHOD_API_ENDPOINT
              + methodExecutionId
              + ARTIFACTS_ENDPOINT
              + (errorScreenshot ? "/error-screenshot" : "");
      LOGGER.info(
          "Uploading {} {} to method execution {} - {}",
          errorScreenshot ? "error screenshot" : "artifact",
          artifact.getName(),
          methodExecutionId,
          executionName);
      String fileName = errorScreenshot ? "error-" + artifact.getName() : artifact.getName();
      try {
        postExecutionArtifact(url, artifact, fileName, true);
      } catch (AqaAgentException aqaException) {
        if (aqaException.shouldThrowException()) {
          LOGGER.error("Error uploading artifact: {}", aqaException.getMessage());
          throw new RuntimeException(aqaException);
        }
      }
    }
  }
}
