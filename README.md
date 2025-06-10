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
`agent.properties` file from `resources` directory or via environment variables.

**Properties set as an environment variable will override those set up in `agent.properties`.**

Supported properties:
| Property name | Type | Default value | Mandatory | Description |
| ------------- | :---: | :---: | :---: | ------------- |
| `aqa-trace.enabled`  | boolean | false | false | |
| `aqa-trace.organization`  | string | / | true | Under what organization name are tests executed |
| `aqa-trace.save-parameter-prefix`  | string | / | false | If you want to save additional execution
information |
| `aqa-trace.fail-suite-on-configuration-failures`  | boolean | false | false | Mark suite
execution as failed if any tests are skipped |
| `aqa-trace.server.hostname`  | string | / | true | URL of AQA Trace server |
| `aqa-trace.artifacts.enabled`  | boolean | false | false | Upload artifacts to AQA Trace * |
| `aqa-trace.artifacts.test-error-screenshot`  | boolean | false | false | Upload test error
screenshot * |
| `aqa-trace.artifacts.configuration-error-screenshot`  | boolean | false | false | Upload
configuration error screenshot * |
| `aqa-trace.logging.enabled`  | boolean | false | false | Upload logs to AQA Trace |
| `aqa-trace.logging.only-from-package`  | list of strings | / | false | Only log logs from provided
packages |
| `aqa-trace.logging.exclude-from-package`  | list of strings | 'com.aqanetics.agent' | false |
Exclude logs from provided packages |

