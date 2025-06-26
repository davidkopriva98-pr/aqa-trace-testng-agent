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
`aqa-trace-agent.properties` file from `resources` directory or via environment variables.

**Properties set as an environment variable will override those set up
in `aqa-trace-agent.properties`.**

### Using `aqa-trace-agent.properties`

Create a file named `aqa-trace-agent.properties` in your `src/main/resources` directory (or
equivalent
for your build system).
Example:

```bash
aqa.trace.enabled=true
aqa.trace.server.hostname=http://localhost:8080
aqa.trace.organization-name=MyOrganization
aqa.trace.artifacts.enabled=true
aqa.trace.logging.exclude-packages=com.another.package
```

### Using Environment Variables

For each property, an equivalent environment variable can be set. The naming convention for
environment variables is derived by converting the property key to uppercase, replacing `.` with
`_`,
and replacing `-` with `_`.

Example:

* Property: `aqa-trace.server.hostname`
* Environment Variable: `AQA_TRACE_SERVER_HOSTNAME`

Supported properties:
| Property name | Environment Variable Name | Type | Default value | Mandatory | Description |
| ---------------------------------------------------------- | ---------------------------------------------------------- | --------------- | -------------------- | --------- | ------------------------------------------------------------------------------------------------------------------------- |
| aqa-trace.enabled | AQA_TRACE_ENABLED | boolean | FALSE | FALSE | Enables or disables the AQA
Trace agent. |
| aqa-trace.stop-execution-when-unreachable | AQA_TRACE_STOP_EXECUTION_WHEN_UNREACHABLE | boolean |
FALSE | FALSE | If true, test execution will stop if the AQA Trace server is unreachable. |
| aqa-trace.organization-name | AQA_TRACE_ORGANIZATION_NAME | string | / | TRUE | Specifies the
organization name under which tests are executed in AQA Trace. |
| aqa-trace.save-parameter-prefix | AQA_TRACE_SAVE_PARAMETER_PREFIX | string | / | FALSE | Defines a
prefix for saving additional execution information. |
| aqa-trace.fail-suite-on-configuration-failures | AQA_TRACE_FAIL_SUITE_ON_CONFIGURATION_FAILURES |
boolean | FALSE | FALSE | If true, the suite execution will be marked as failed if any tests are
skipped due to configuration failures. |
| aqa-trace.server.hostname | AQA_TRACE_SERVER_HOSTNAME | string | / | TRUE | The URL of the AQA
Trace server (e.g., http://localhost:8080). |
| aqa-trace.artifacts.enabled | AQA_TRACE_ARTIFACTS_ENABLED | boolean | FALSE | FALSE | If true,
enables the upload of artifacts to AQA Trace. |
| aqa-trace.artifacts.test-error-screenshot-enabled |
AQA_TRACE_ARTIFACTS_TEST_ERROR_SCREENSHOT_ENABLED | boolean | FALSE | FALSE | If true, uploads
screenshots taken on test errors to AQA Trace. Requires aqa-trace.artifacts.enabled to be true. |
| aqa-trace.artifacts.configuration-error-screenshot-enabled |
AQA_TRACE_ARTIFACTS_CONFIGURATION_ERROR_SCREENSHOT_ENABLED | boolean | FALSE | FALSE | If true,
uploads screenshots taken on configuration errors to AQA Trace. Requires aqa-trace.artifacts.enabled
to be true. |
| aqa-trace.logging.enabled | AQA_TRACE_LOGGING_ENABLED | boolean | FALSE | FALSE | If true, enables
the upload of logs to AQA Trace. |
| aqa-trace.logging.include-packages | AQA_TRACE_LOGGING_INCLUDE_PACKAGES | list of strings | / |
FALSE | A comma-separated list of package prefixes. Only logs originating from these packages will
be uploaded. |
| aqa-trace.logging.exclude-packages | AQA_TRACE_LOGGING_EXCLUDE_PACKAGES | list of strings |
com.aqanetics.agent' | FALSE | A comma-separated list of package prefixes. Logs originating from
these packages will be excluded from upload. |

