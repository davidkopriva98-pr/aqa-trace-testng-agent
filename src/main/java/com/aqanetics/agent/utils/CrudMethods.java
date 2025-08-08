package com.aqanetics.agent.utils;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.LOG_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.METHOD_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.core.exception.AqaAgentException;
import com.aqanetics.agent.testng.ExecutionEntities;
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

  private static final boolean STOP_WHEN_UNREACHABLE =
      AqaConfigLoader.getBooleanProperty("aqa-trace.stop-execution-when-unreachable", false);

  public static String sendPost(URI uri, String postBody) throws AqaAgentException {
    if (AqaTraceServerGuards.isEnabled()) {
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        HttpEntity requestEntity = new StringEntity(postBody, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);

        return httpClient.execute(
            httpPost,
            response -> {
              int statusCode = response.getCode();
              LOGGER.debug("API call to '{}' returned code: {}", uri, statusCode);

              HttpEntity responseEntity = response.getEntity();
              String responseBody = null;
              if (responseEntity != null) {
                try {
                  responseBody = EntityUtils.toString(responseEntity);
                } catch (ParseException e) {
                  LOGGER.error(
                      "Failed to parse response entity from '{}': {}", uri, e.getMessage());
                }
              }
              if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
              } else {
                String errorMessage =
                    String.format(
                        "API call to '%s' failed with status code %d. Response: %s",
                        uri, statusCode, responseBody != null ? responseBody : "N/A");
                LOGGER.error(errorMessage);
                throw new AqaAgentException(errorMessage, !STOP_WHEN_UNREACHABLE);
              }
            });
      } catch (IOException e) {
        LOGGER.error("Failed API call to '{}': {}", uri, e.getMessage());
        throw new AqaAgentException(e.getMessage(), !STOP_WHEN_UNREACHABLE);
      }
    } else {
      LOGGER.debug("API endpoint not configured or reporting disabled");
      return null;
    }
  }

  public static String sendSuitePatch(Map<String, Object> updatedParameters)
      throws AqaAgentException {
    if (AqaTraceServerGuards.isEnabled() && AqaTraceServerGuards.isSuiteExecIdSet()) {
      try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpPatch httpPatch =
            new HttpPatch(
                URI.create(
                    API_ENDPOINT
                        + AGENT_API_ENDPOINT
                        + SUITE_API_ENDPOINT
                        + ExecutionEntities.suiteExecutionId));
        httpPatch.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

        String postBody = AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(updatedParameters);
        HttpEntity requestEntity = new StringEntity(postBody, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(requestEntity);

        return client.execute(
            httpPatch,
            response -> {
              int statusCode = response.getCode();
              LOGGER.debug("API call to 'patch' returned code: {}", statusCode);

              HttpEntity responseEntity = response.getEntity();
              String responseBody = null;
              if (responseEntity != null) {
                try {
                  responseBody = EntityUtils.toString(responseEntity);
                } catch (ParseException e) {
                  LOGGER.error(
                      "Failed to parse response entity from patch endpoint: {}", e.getMessage());
                }
              }
              if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
              } else {
                String errorMessage =
                    String.format(
                        "API call failed with status code %d. Response: %s",
                        statusCode, responseBody != null ? responseBody : "N/A");
                LOGGER.error(errorMessage);
                throw new AqaAgentException(errorMessage, !STOP_WHEN_UNREACHABLE);
              }
            });
      } catch (IOException e) {
        LOGGER.error("Failed API call to patch: {}", e.getMessage());
        throw new AqaAgentException(e.getMessage(), !STOP_WHEN_UNREACHABLE);
      } catch (Exception e) {
        LOGGER.error("Failed to serialize parameters for PATCH request: {}", e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public static void postLog(String postBody) throws AqaAgentException {
    URI uri =
        URI.create(
            API_ENDPOINT
                + AGENT_API_ENDPOINT
                + SUITE_API_ENDPOINT
                + ExecutionEntities.suiteExecutionId
                + METHOD_API_ENDPOINT
                + ExecutionEntities.inProgressMethodExecutionId.toString()
                + LOG_API_ENDPOINT);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(uri);

      httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
      HttpEntity requestEntity = new StringEntity(postBody, ContentType.APPLICATION_JSON);
      httpPost.setEntity(requestEntity);

      client.execute(httpPost, response -> null);

    } catch (IOException e) {
      throw new AqaAgentException(e.getMessage(), !STOP_WHEN_UNREACHABLE);
    }
  }

  public static void postExecutionArtifact(
      String url, File artifact, String fileName, boolean ofMethodExecution)
      throws AqaAgentException {
    if (AqaTraceServerGuards.isEnabled()
        && !AqaTraceServerGuards.isServerUnreachable()
        && ((ofMethodExecution && ExecutionEntities.testExecution != null)
            || (!ofMethodExecution && ExecutionEntities.suiteExecutionId != null))) {
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        HttpEntity multipartEntity =
            MultipartEntityBuilder.create()
                .addBinaryBody("file", artifact, ContentType.DEFAULT_BINARY, fileName)
                .build();
        ClassicHttpRequest httpPost =
            ClassicRequestBuilder.post(url).setEntity(multipartEntity).build();
        Integer responseCode =
            httpClient.execute(httpPost, org.apache.hc.core5.http.HttpResponse::getCode);
        LOGGER.debug("Response Code: {}", responseCode);
        if (responseCode >= 300) {
          String errorMessage =
              String.format("API call to post log failed with status code %d", responseCode);
          LOGGER.error(errorMessage);
          throw new AqaAgentException(errorMessage, !STOP_WHEN_UNREACHABLE);
        }
      } catch (IOException e) {
        LOGGER.debug("Failed to upload artifact: {}", e.getMessage());
        throw new AqaAgentException(e.getMessage(), !STOP_WHEN_UNREACHABLE);
      }
    }
  }
}
