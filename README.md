# AQA Trace TestNG Agent

TestNG agent for AQA Trace. You need to have AQA Trace up & running, if you want to use this agent.

## Installation

Import dependency into the project containing TestNG tests.

Maven:

```xml

<dependency>
  <groupId>com.aqanetics</groupId>
  <artifactId>aqa-trace-testng-agent</artifactId>
  <version>{aqa-trace-testng-agent.version}</version>
</dependency>
```

Gradle:

```bash
implementation group: 'com.aqanetics', name: 'aqa-trace-testng-agent', version: ''
```

## Configuration properties

To use this agent, you will need to set up some properties. Properties can either be set using
`aqa-trace-agent.properties` file from `resources` directory, via system properties or using environment variables.


__❗❗Properties set as a system properties will override environment variables and environment variables will override those set up in `aqa-trace-agent.properties`.❗❗__


### Using `aqa-trace-agent.properties`

Create a file named `aqa-trace-agent.properties` in your `src/main/resources` directory (or
equivalent
for your build system).
Example:

```bash
aqa-trace.enabled=true
aqa-trace.server.hostname=http://localhost:8080
aqa-trace.organization-name=MyOrganization
aqa-trace.artifacts.enabled=true
aqa-trace.logging.exclude-packages=com.another.package
```

### Using System Properties

When starting a new execution, pass desired properties using `-D` flag. Example:
```bash
-Daqa-trace.server.hostname=http://localhost:8080
```

### Using Environment Variables

For each property, an equivalent environment variable can be set. The naming convention for
environment variables is derived by converting the property key to uppercase, replacing `.` with
`_`, and replacing `-` with `_`.

Example:

* Property: `aqa-trace.server.hostname`
* Environment Variable: `AQA_TRACE_SERVER_HOSTNAME`

Supported properties:
| Property name | Environment Variable Name | Type | Default value | Mandatory | Description |
| ---------------------------------------------------------- | ---------------------------------------------------------- | --------------- | -------------------- | --------- | ------------------------------------------------------------------------------------------------------------------------- |
| `aqa-trace.enabled` | `AQA_TRACE_ENABLED` | `boolean` | FALSE | FALSE | Enables or disables the AQA Trace agent. |
| `aqa-trace.stop-execution-when-unreachable` | `AQA_TRACE_STOP_EXECUTION_WHEN_UNREACHABLE` | `boolean` | FALSE | FALSE | If true, test execution will stop if the AQA Trace server is unreachable. |
| `aqa-trace.organization-name` | `AQA_TRACE_ORGANIZATION_NAME` | `string` | / | TRUE | Specifies the organization name under which tests are executed in AQA Trace. |
| `aqa-trace.save-parameter-prefix` | `AQA_TRACE_SAVE_PARAMETER_PREFIX` | `string` | "AQA" | FALSE | Defines a prefix for saving additional execution information. |
| `aqa-trace.fail-suite-on-configuration-failures` | `AQA_TRACE_FAIL_SUITE_ON_CONFIGURATION_FAILURES` | `boolean` | FALSE | FALSE | If true, the suite execution will be marked as failed if any tests are skipped due to configuration failures. |
| `aqa-trace.server.hostname` | `AQA_TRACE_SERVER_HOSTNAME` | `string` | / | TRUE | The URL of the AQA Trace server (e.g., http://localhost:8080). |
| `aqa-trace.artifacts.enabled` | `AQA_TRACE_ARTIFACTS_ENABLED` | `boolean` | FALSE | FALSE | If true, enables the upload of artifacts to AQA Trace. |
| `aqa-trace.artifacts.test-error-screenshot-enabled` | `AQA_TRACE_ARTIFACTS_TEST_ERROR_SCREENSHOT_ENABLED` | `boolean` | FALSE | FALSE | If true, uploads screenshots taken on test errors to AQA Trace. Requires aqa-trace.artifacts.enabled to be true. |
| `aqa-trace.artifacts.configuration-error-screenshot-enabled` | `AQA_TRACE_ARTIFACTS_CONFIGURATION_ERROR_SCREENSHOT_ENABLED` | `boolean` | FALSE | FALSE | If true, uploads screenshots taken on configuration errors to AQA Trace. Requires aqa-trace.artifacts.enabled to be true. |
| `aqa-trace.logging.enabled` | `AQA_TRACE_LOGGING_ENABLED` | `boolean` | FALSE | FALSE | If true, enables the upload of logs to AQA Trace. |
| `aqa-trace.logging.include-packages` | `AQA_TRACE_LOGGING_INCLUDE_PACKAGES` | `string[]` | / | FALSE | A comma-separated list of package prefixes. Only logs originating from these packages will be uploaded. |
| `aqa-trace.logging.exclude-packages` | `AQA_TRACE_LOGGING_EXCLUDE_PACKAGES` | `string[]` | com.aqanetics.agent | FALSE | A comma-separated list of package prefixes. Logs originating from these packages will be excluded from upload. |

## Usage

### Omitting logs

If you only want logs from specific packages, you can use `AQA_TRACE_LOGGING_INCLUDE_PACKAGES` property. On the other hand, if you want all logs, except from one (or more - comma separated) package, use `AQA_TRACE_LOGGING_EXCLUDE_PACKAGES`.

### Detecting parameters in `.xml` file

The agent has capability to detect and store parameter information. Parameters are read from `suite.xml` file that is used to start execution(s). Example from `xml` file:

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd" >
<suite annotations="JDK" name="Test Execution" verbose="1">
  <parameter name="aqa_environment" value='{"value": "FooBar", "number": 42}'/>
  <parameter name="aqa_username" value='{"value": "johnny-english"}'/>
  <parameter name="aqa_machine" value='{"number": 4}'/>
  ...
</suite>
```

Value must be in json format: ```json {value: string | null, number: number | null}```. One, either value or number must be present, otherwise parameter will not be detected by AQA Trace Agent.

### Ignoring specific tests / configurations

With AQA Trace Agent you can skip any `@Test` or `@Before` / `@After` methods from being registered with AQA Trace, by using `@AQATraceIgnore` annotation. Method won't be included in any statistic, logs won't be stored and artifacts won't be saved. 

```java
@BeforeMethod
@AQATraceIgnore
public void openBrowser() {
  //some code here
}
```
