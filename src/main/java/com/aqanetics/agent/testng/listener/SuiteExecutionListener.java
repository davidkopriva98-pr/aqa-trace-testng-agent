package com.aqanetics.agent.testng.listener;

import static com.aqanetics.AqaConfigLoader.AGENT_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.ARTIFACTS_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.SUITE_API_ENDPOINT;
import static com.aqanetics.AqaConfigLoader.getProperty;
import static com.aqanetics.utils.CrudMethods.postExecutionArtifact;

import com.aqanetics.AqaConfigLoader;
import com.aqanetics.agent.testng.ExecutionEntities;
import com.aqanetics.agent.testng.dto.NewSuiteExecutionDto;
import com.aqanetics.agent.testng.dto.OrganizationDto;
import com.aqanetics.agent.testng.dto.ParameterDto;
import com.aqanetics.utils.CrudMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Objects;
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
      AqaConfigLoader.getProperty("aqa-trace.save-parameter-prefix", null);
  private static final boolean FAIL_SUITE_ON_CONF_FAIL =
      Objects.equals(
          AqaConfigLoader.getProperty("aqa-trace.fail-suite-on-configuration-failures", "false"),
          "true");
  private static final boolean ENABLED_ARTIFACTS =
      Objects.equals(AqaConfigLoader.getProperty("aqa-trace.artifacts.enabled", "false"), "true");

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
    List<ParameterDto> parameters = new ArrayList<>();
    if (SAVE_PARAMETER_PREFIX != null) {
      Map<String, String> allParameters = suite.getXmlSuite().getAllParameters();
      List<String> keys =
          allParameters.keySet().stream()
              .filter((key) -> key.toLowerCase().startsWith(SAVE_PARAMETER_PREFIX.toLowerCase()))
              .toList();
      keys.forEach(
          (key) -> {
            String[] values = allParameters.get(key).split(";");
            String valueText = values[0];
            Long valueNumeric = Long.valueOf(values[1]);
            parameters.add(
                new ParameterDto(
                    key.replace(SAVE_PARAMETER_PREFIX + "_", ""), valueNumeric, valueText));
          });
    }

    OrganizationDto organizationDto =
        new OrganizationDto(getProperty("aqa-trace.organization", "unknown"));
    LOGGER.info(organizationDto.toString());

    NewSuiteExecutionDto newSuiteExecution =
        new NewSuiteExecutionDto(Instant.now(), true, parameters, organizationDto, hostName);

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
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      LOGGER.error("Error parsing suiteExecution: {}", e.getMessage());
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

    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void onFinish(ISuite suite) {
    if (ExecutionEntities.suiteExecutionId != null) {
      uploadXmlSuiteFile(suite.getXmlSuite());
      LOGGER.info("SuiteExecution with id: {} is finished.", ExecutionEntities.suiteExecutionId);
      Map<String, Object> jsonPayload = new HashMap<>();
      jsonPayload.put("endTime", Instant.now().toString());
      jsonPayload.put("inProgress", false);
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
      } catch (IOException e) {
        LOGGER.error("Error uploading suite execution", e);
      } finally {
        assert suiteFile != null;
        suiteFile.delete();
      }
    }
  }
}
