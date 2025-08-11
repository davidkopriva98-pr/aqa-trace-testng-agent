# AQA Trace TestNG Agent

The **TestNG Agent** for **AQA Trace** — enabling seamless reporting of test execution data to the AQA Trace server.

> **Note:** You must have an AQA Trace server running before using this agent.

---

## 📦 Installation

Add the dependency to your **TestNG test project**.

**Maven:**
```xml
<dependency>
  <groupId>com.aqanetics</groupId>
  <artifactId>aqa-trace-testng-agent</artifactId>
  <version>{aqa-trace-testng-agent.version}</version>
</dependency>
```

**Gradle:**

```bash
implementation group: 'com.aqanetics', name: 'aqa-trace-testng-agent', version: '{version}'
```

## ⚙️ Configuration

The agent can be configured using `aqa-trace-agent.properties`, **system properties**, or **environment variables**.

❗ Priority order (highest wins):

1. System properties (-D...)
2. Environment variables
3. aqa-trace-agent.properties file

### 1️⃣ Using `aqa-trace-agent.properties`

Place the file in `src/main/resources`:

```bash
aqa-trace.enabled=true
aqa-trace.server.hostname=http://localhost:8080
aqa-trace.organization-name=MyOrganization
aqa-trace.artifacts.enabled=true
aqa-trace.logging.exclude-packages=com.another.package
```

### 2️⃣ Using System Properties

Pass properties when running tests:
```bash
-Daqa-trace.server.hostname=http://localhost:8080
```

### 3️⃣ Using Environment Variables

Convert property names:
- . → _
- - → _
- Uppercase everything

Example:

* Property: `aqa-trace.server.hostname`
* Environment Variable: `AQA_TRACE_SERVER_HOSTNAME`

| Property                                                     | Environment Variable                                         | Type      | Default             | Mandatory | Description                                                                     |
|--------------------------------------------------------------|--------------------------------------------------------------|-----------|---------------------|-----------|---------------------------------------------------------------------------------|
| `aqa-trace.enabled`                                          | `AQA_TRACE_ENABLED`                                          | boolean   | false               | ❌         | Enable/disable the agent.                                                       |
| `aqa-trace.stop-execution-when-unreachable`                  | `AQA_TRACE_STOP_EXECUTION_WHEN_UNREACHABLE`                  | boolean   | false               | ❌         | Stop tests if AQA Trace server is unreachable.                                  |
| `aqa-trace.organization-name`                                | `AQA_TRACE_ORGANIZATION_NAME`                                | string    | —                   | ✅         | Organization name for execution context.                                        |
| `aqa-trace.save-parameter-prefix`                            | `AQA_TRACE_SAVE_PARAMETER_PREFIX`                            | string    | AQA                 | ❌         | Prefix for saving extra execution info.                                         |
| `aqa-trace.fail-suite-on-configuration-failures`             | `AQA_TRACE_FAIL_SUITE_ON_CONFIGURATION_FAILURES`             | boolean   | false               | ❌         | Mark suite as failed if configuration errors occur.                             |
| `aqa-trace.server.hostname`                                  | `AQA_TRACE_SERVER_HOSTNAME`                                  | string    | —                   | ✅         | AQA Trace server URL.                                                           |
| `aqa-trace.artifacts.enabled`                                | `AQA_TRACE_ARTIFACTS_ENABLED`                                | boolean   | false               | ❌         | Enable artifact uploads.                                                        |
| `aqa-trace.artifacts.test-error-screenshot-enabled`          | `AQA_TRACE_ARTIFACTS_TEST_ERROR_SCREENSHOT_ENABLED`          | boolean   | false               | ❌         | Upload screenshots for test errors. Requires `artifacts.enabled=true`.          |
| `aqa-trace.artifacts.configuration-error-screenshot-enabled` | `AQA_TRACE_ARTIFACTS_CONFIGURATION_ERROR_SCREENSHOT_ENABLED` | boolean   | false               | ❌         | Upload screenshots for configuration errors. Requires `artifacts.enabled=true`. |
| `aqa-trace.logging.enabled`                                  | `AQA_TRACE_LOGGING_ENABLED`                                  | boolean   | false               | ❌         | Enable log uploads.                                                             |
| `aqa-trace.logging.include-packages`                         | `AQA_TRACE_LOGGING_INCLUDE_PACKAGES`                         | string\[] | —                   | ❌         | Comma-separated package prefixes to include.                                    |
| `aqa-trace.logging.exclude-packages`                         | `AQA_TRACE_LOGGING_EXCLUDE_PACKAGES`                         | string\[] | com.aqanetics.agent | ❌         | Comma-separated package prefixes to exclude.                                    |


## 🛠 Usage

### 🎯 Controlling log uploads

* To include only specific packages: set AQA_TRACE_LOGGING_INCLUDE_PACKAGES
* To exclude specific packages: set AQA_TRACE_LOGGING_EXCLUDE_PACKAGES

### 📑 Parameter detection from TestNG XML

The agent can detect execution parameters from your `suite.xml` file.

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

### 🚫 Ignoring specific tests / configurations

Use `@AQATraceIgnore` on any `@Test`, `@Before*`, or `@After*` method to **exclude** it from:

* Execution statistics
* Logs
* Artifacts

Example: 

```java
@BeforeMethod
@AQATraceIgnore
public void openBrowser() {
  //some code here
}
```

## 📄 License

Licensed under the MIT License — see the LICENSE file for details.