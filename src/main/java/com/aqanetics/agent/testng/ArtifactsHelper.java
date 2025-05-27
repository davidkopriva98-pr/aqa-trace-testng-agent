package com.aqanetics.agent.testng;

import static com.aqanetics.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.TEST_API_ENDPOINT;

import com.aqanetics.AqaConfigLoader;
import com.aqanetics.exception.ArtifactException;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactsHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsHelper.class);
  private static final boolean ENABLED_ARTIFACTS = Objects.equals(
      AqaConfigLoader.getProperty("aqa-trace.artifacts.enabled", "false"), "true");
  private static final boolean ENABLED_TEST_SCREENSHOT = Objects.equals(
      AqaConfigLoader.getProperty("aqa-trace.artifacts.test-error-screenshot", "false"), "true");
  private static final boolean ENABLED_CONFIGURATION_SCREENSHOT = Objects.equals(
      AqaConfigLoader.getProperty("aqa-trace.artifacts.configuration-error-screenshot", "false"),
      "true");

  public static void uploadConfigurationArtifact(File artifact) {
    uploadFile(artifact, ExecutionEntities.currentNotTestExecution.id(),
        ExecutionEntities.currentNotTestExecution.testName(), false);
  }

  public static void uploadTestArtifact(File artifact) {
    uploadFile(artifact, ExecutionEntities.testExecution.id(),
        ExecutionEntities.testExecution.testName(), false);
  }

  public static void uploadTestErrorScreenshot(File artifact) {
    if (ENABLED_TEST_SCREENSHOT) {
      uploadFile(artifact, ExecutionEntities.testExecution.id(),
          ExecutionEntities.testExecution.testName(), true);
    }
  }

  public static void uploadConfigurationErrorScreenshot(File artifact) {
    if (ENABLED_CONFIGURATION_SCREENSHOT) {
      uploadFile(artifact, ExecutionEntities.currentNotTestExecution.id(),
          ExecutionEntities.currentNotTestExecution.testName(), true);
    }
  }

  private static void uploadFile(File artifact, Long executionId, String executionName,
      boolean errorScreenshot) {
    if (ENABLED_ARTIFACTS) {
      String url = AqaConfigLoader.API_ENDPOINT + AGENT_API_ENDPOINT + SUITE_API_ENDPOINT
                   + ExecutionEntities.suiteExecutionId + TEST_API_ENDPOINT + executionId
                   + "/artifacts" + (errorScreenshot ? "/error-screenshot" : "");
      LOGGER.info("Uploading {} {} to test execution {}",
          errorScreenshot ? "error screenshot" : "artifact", artifact.getName(), executionName);
      postArtifact(url, artifact, artifact.getName());
    }
  }

  private static void postArtifact(String url, File artifact, String fileName) {
    if (AqaConfigLoader.API_ENDPOINT != null && ExecutionEntities.suiteExecutionId != null
        && ExecutionEntities.testExecution != null) {
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        HttpEntity multipartEntity = MultipartEntityBuilder.create()
            .addBinaryBody("file", artifact, ContentType.DEFAULT_BINARY, fileName)
            .build();
        ClassicHttpRequest httpPost = ClassicRequestBuilder.post(url).setEntity(multipartEntity)
            .build();
        Integer responseCode = httpClient.execute(httpPost, HttpResponse::getCode);
        LOGGER.info("Response Code: {}", responseCode);
      } catch (IOException e) {
        LOGGER.info("Failed to upload artifact: {}", e.getMessage());
        throw new ArtifactException(e.getMessage());
      }
    }
  }
}
