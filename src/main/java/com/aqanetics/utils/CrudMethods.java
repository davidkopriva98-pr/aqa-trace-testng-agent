package com.aqanetics.utils;

import static com.aqanetics.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.SUITE_API_ENDPOINT;

import com.aqanetics.AqaConfigLoader;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.exception.ArtifactException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrudMethods {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrudMethods.class);

  public static String sendPost(URI uri, String postBody) {
    if (AqaConfigLoader.ENABLED && AqaConfigLoader.API_ENDPOINT != null) {
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        HttpEntity requestEntity = new StringEntity(postBody, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);

        return httpClient.execute(httpPost, response -> {
          int statusCode = response.getCode();
          LOGGER.debug("API call to '{}' returned code: {}", uri, statusCode);

          HttpEntity responseEntity = response.getEntity();
          if (responseEntity != null) {
            try {
              return EntityUtils.toString(responseEntity);
            } catch (ParseException e) {
              LOGGER.error("Failed to parse response entity from '{}': {}", uri, e.getMessage());
              return null;
            }
          }
          LOGGER.error("Response from '{}' is null", uri);
          return null;
        });
      } catch (IOException e) {
        LOGGER.error("Failed API call to '{}': {}", uri, e.getMessage());
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

      try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpPatch httpPatch = new HttpPatch(URI.create(
            AqaConfigLoader.API_ENDPOINT + AGENT_API_ENDPOINT + SUITE_API_ENDPOINT
            + ExecutionEntities.suiteExecutionId));
        httpPatch.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

        String postBody = AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(updatedParameters);
        HttpEntity requestEntity = new StringEntity(postBody, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(requestEntity);

        return client.execute(httpPatch, response -> {
          int statusCode = response.getCode();
          LOGGER.debug("API call to 'patch' returned code: {}", statusCode);

          HttpEntity responseEntity = response.getEntity();
          if (responseEntity != null) {
            try {
              return EntityUtils.toString(responseEntity);
            } catch (ParseException e) {
              LOGGER.error("Failed to parse response entity from patch endpoint: {}",
                  e.getMessage());
              return null;
            }
          }
          LOGGER.error("Response from 'patch' is null");
          return null;
        });
      } catch (IOException e) {
        LOGGER.error("Failed API call to patch: {}", e.getMessage());
        return null;
      } catch (Exception e) {
        LOGGER.error("Failed to serialize parameters for PATCH request: {}", e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public static void postLog(String postBody) {
    URI uri = URI.create(
        AqaConfigLoader.API_ENDPOINT + AqaConfigLoader.AGENT_API_ENDPOINT
        + AqaConfigLoader.TEST_API_ENDPOINT
        + ExecutionEntities.inProgressTestExecutionId.toString()
        + AqaConfigLoader.LOG_API_ENDPOINT);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(uri);

      httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
      HttpEntity requestEntity = new StringEntity(postBody, ContentType.APPLICATION_JSON);
      httpPost.setEntity(requestEntity);

      client.execute(httpPost, response -> null);

    } catch (IOException e) {
      throw new RuntimeException("Failed to send log POST request", e);
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
