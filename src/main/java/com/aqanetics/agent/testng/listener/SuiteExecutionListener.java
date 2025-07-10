package com.aqanetics.agent.testng.listener;

import static com.aqanetics.agent.config.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.ARTIFACTS_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.agent.config.AqaConfigLoader.getProperty;
import static com.aqanetics.agent.utils.CrudMethods.postExecutionArtifact;

import com.aqanetics.agent.config.AqaConfigLoader;
import com.aqanetics.agent.core.dto.NewSuiteExecutionDto;
import com.aqanetics.agent.core.exception.AqaAgentException;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.agent.utils.CrudMethods;
import com.aqanetics.dto.basic.BasicSuiteExecutionParameterDto;
import com.aqanetics.dto.normal.OrganizationDto;
import com.aqanetics.enums.ExecutionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.xml.XmlSuite;

public class SuiteExecutionListener implements ISuiteListener, IInvokedMethodListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SuiteExecutionListener.class);
  private static final String SAVE_PARAMETER_PREFIX =
      AqaConfigLoader.getProperty("aqa-trace.save-parameter-prefix", "AQA");
  private static final boolean FAIL_SUITE_ON_CONF_FAIL =
      AqaConfigLoader.getBooleanProperty("aqa-trace.fail-suite-on-configuration-failures", false);
  private static final boolean ENABLED_ARTIFACTS =
      AqaConfigLoader.getBooleanProperty("aqa-trace.artifacts.enabled", false);

  public SuiteExecutionListener() {}

  @Override
  public void onStart(ISuite suite) {
    String hostName = null;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      // Do nothing
    }
    LOGGER.info("Suite '{}' is starting (on host {})...", suite.getName(), hostName);
    this.registerNewSuiteExecution(suite, hostName);
  }

  /**
   * Reads all parameters from xml suite and creates {@link NewSuiteExecutionDto} with all required
   * data and sends it to AQA-trace server.
   *
   * @param suite testNG suite entity
   */
  private void registerNewSuiteExecution(ISuite suite, String hostName) {
    List<BasicSuiteExecutionParameterDto> parameters = new ArrayList<>();
    if (SAVE_PARAMETER_PREFIX != null) {
      Map<String, String> allParameters = suite.getXmlSuite().getAllParameters();
      List<String> keys =
          allParameters.keySet().stream()
              .filter((key) -> key.toUpperCase().startsWith(SAVE_PARAMETER_PREFIX.toUpperCase()))
              .toList();
      keys.forEach(
          (key) -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
              Map<String, Object> parameter = mapper.readValue(allParameters.get(key), Map.class);

              Long number = null;
              String value = null;

              if (parameter.containsKey("number")) {
                number = Long.valueOf(parameter.get("number").toString());
              }
              if (parameter.containsKey("value")) {
                value = parameter.get("value").toString();
              }

              parameters.add(
                  new BasicSuiteExecutionParameterDto(
                      null,
                      key.toUpperCase().replace(SAVE_PARAMETER_PREFIX.toUpperCase() + "_", ""),
                      number,
                      value));
            } catch (JsonProcessingException e) {
              LOGGER.warn("Failed to parse parameter {}", allParameters.get(key));
              LOGGER.warn(e.getMessage());
            }
          });
    }

    OrganizationDto organizationDto =
        new OrganizationDto(null, getProperty("aqa-trace.organization-name", "unknown"));
    LOGGER.info(organizationDto.toString());

    NewSuiteExecutionDto newSuiteExecution =
        new NewSuiteExecutionDto(
            Instant.now(), parameters, organizationDto, hostName, ExecutionStatus.IN_PROGRESS);

    LOGGER.info("Posting new suite execution {}", newSuiteExecution);

    try {
      String response =
          CrudMethods.sendPost(
              URI.create(API_ENDPOINT + AGENT_API_ENDPOINT + SUITE_API_ENDPOINT + "new"),
              AqaConfigLoader.OBJECT_MAPPER.writeValueAsString(newSuiteExecution));
      if (response != null) {
        JsonNode rootNode = AqaConfigLoader.OBJECT_MAPPER.readTree(response);
        ExecutionEntities.suiteExecutionId = rootNode.get("id").asLong();
        LOGGER.info(
            "Registered new suite execution with id: {}", ExecutionEntities.suiteExecutionId);
      }
    } catch (AqaAgentException aqaException) {
      if (aqaException.shouldThrowException()) {
        LOGGER.error("Error creating new suiteExecution: {}", aqaException.getMessage());
        throw new RuntimeException(aqaException);
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while JSON parsing suiteExecution: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private void updateSuiteExecution(Map<String, Object> jsonPayload) {
    try {
      String response = CrudMethods.sendSuitePatch(jsonPayload);
      if (response != null) {
        JsonNode rootNode = AqaConfigLoader.OBJECT_MAPPER.readTree(response);
        ExecutionEntities.suiteExecutionId = rootNode.get("id").asLong();
        LOGGER.debug(
            "Received updated suiteExecution with id: {}", ExecutionEntities.suiteExecutionId);
      }

    } catch (AqaAgentException aqaException) {
      if (aqaException.shouldThrowException()) {
        LOGGER.error("Error updating new suiteExecution: {}", aqaException.getMessage());
        throw new RuntimeException(aqaException);
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while JSON parsing suiteExecution: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public void onFinish(ISuite suite) {
    if (ExecutionEntities.suiteExecutionId != null) {
      uploadXmlSuiteFile(suite.getXmlSuite());
      LOGGER.info("SuiteExecution with id: {} is finished.", ExecutionEntities.suiteExecutionId);
      Map<String, Object> jsonPayload = new HashMap<>();
      jsonPayload.put("endTime", Instant.now().toString());
      boolean allPassed =
          suite.getResults().values().stream()
              .allMatch(
                  (result) ->
                      result.getTestContext().getFailedTests().size() == 0
                          && result.getTestContext().getSkippedTests().size() == 0
                          && (!FAIL_SUITE_ON_CONF_FAIL
                              || (result.getTestContext().getFailedConfigurations().size() == 0
                                  && result.getTestContext().getFailedConfigurations().size()
                                      == 0)));

      jsonPayload.put("status", allPassed ? "PASSED" : "FAILED");
      this.updateSuiteExecution(jsonPayload);
    }
  }

  private void uploadXmlSuiteFile(XmlSuite xmlSuite) {
    LOGGER.info("Uploading XML suite file {} - {}", xmlSuite.getName(), ENABLED_ARTIFACTS);
    if (ENABLED_ARTIFACTS) {
      String url =
          API_ENDPOINT
              + AGENT_API_ENDPOINT
              + SUITE_API_ENDPOINT
              + ExecutionEntities.suiteExecutionId
              + ARTIFACTS_ENDPOINT;
      LOGGER.info(
          "Uploading suite execution {} to suite execution {}",
          xmlSuite.getName(),
          ExecutionEntities.suiteExecutionId);

      File suiteFile = null;
      try {
        suiteFile = File.createTempFile(UUID.randomUUID().toString(), ".xml");
        Path path = suiteFile.toPath();
        Files.write(path, xmlSuite.toXml().getBytes());

        postExecutionArtifact(url, suiteFile, "suite.xml", false);
      } catch (AqaAgentException aqaException) {
        if (aqaException.shouldThrowException()) {
          LOGGER.error("Error uploading suiteExecution xml file: {}", aqaException.getMessage());
          throw new RuntimeException(aqaException);
        }
      } catch (IOException e) {
        LOGGER.error("Error IO operation for xml file: {}", e.getMessage());
        throw new RuntimeException(e);
      } finally {
        assert suiteFile != null;
        suiteFile.delete();
      }
    }
  }
}
