package com.aqanetics.utils;

import static com.aqanetics.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.LOG_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.TEST_API_ENDPOINT;

import com.aqanetics.AqaConfigLoader;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.exception.ArtifactException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrudMethods {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrudMethods.class);

  public static String sendPost(URI uri, String postBody) {
    if (AqaConfigLoader.ENABLED && AqaConfigLoader.API_ENDPOINT != null) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(uri)
            .header("Content-Type", "application/json").POST(BodyPublishers.ofString(postBody))
            .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        LOGGER.debug("API call to '{}' returned code: {}", uri.toString(), response.statusCode());
        return response.body();
      } catch (Exception e) {
        LOGGER.error("Failed API call to '{}': {}", uri.toString(), e.getMessage());
        return null;
      }
    } else {
      LOGGER.info("API endpoint not configured or reporting disabled");
      return null;
    }
  }

  public static String sendSuitePatch(Map<String, Object> updatedParameters) {
    if (AqaConfigLoader.ENABLED && ExecutionEntities.suiteExecutionId != null
        && AqaConfigLoader.API_ENDPOINT != null) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(
                AqaConfigLoader.API_ENDPOINT + AGENT_API_ENDPOINT + SUITE_API_ENDPOINT
                + ExecutionEntities.suiteExecutionId)).header("Content-Type", "application/json")
            .method("PATCH", BodyPublishers.ofString(
                AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(updatedParameters))).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        LOGGER.debug("API call to 'patch' returned code: {}", response.statusCode());
        return response.body();
      } catch (Exception e) {
        LOGGER.error("Failed API call to patch: {}", e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public static void postLog(String postBody) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().uri(
              URI.create(AqaConfigLoader.API_ENDPOINT + AGENT_API_ENDPOINT + TEST_API_ENDPOINT
                         + ExecutionEntities.inProgressTestExecutionId.toString() + LOG_API_ENDPOINT))
          .header("Content-Type", "application/json").POST(
              BodyPublishers.ofString(postBody)).build();
      client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void postExecutionArtifact(String url, File artifact, String fileName,
      boolean ofTestExecution) {
    if (AqaConfigLoader.API_ENDPOINT != null && (
        (ofTestExecution && ExecutionEntities.testExecution != null) || (!ofTestExecution &&
                                                                         ExecutionEntities.suiteExecutionId
                                                                         != null))) {
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        HttpEntity multipartEntity = MultipartEntityBuilder.create()
            .addBinaryBody("file", artifact, ContentType.DEFAULT_BINARY, fileName)
            .build();
        ClassicHttpRequest httpPost = ClassicRequestBuilder.post(url).setEntity(multipartEntity)
            .build();
        Integer responseCode = httpClient.execute(httpPost,
            org.apache.hc.core5.http.HttpResponse::getCode);
        LOGGER.info("Response Code: {}", responseCode);
      } catch (IOException e) {
        LOGGER.info("Failed to upload artifact: {}", e.getMessage());
        throw new ArtifactException(e.getMessage());
      }
    }
  }
}
