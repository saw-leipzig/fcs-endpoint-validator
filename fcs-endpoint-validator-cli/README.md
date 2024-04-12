# FCS Endpoint Validator CLI

This CLI tool allows to batch validate a list of endpoints with adjustable validation configurations.

## How to Build

It is easiest to just build the whole parent project since the `fcs-endpoint-validator-core` library is required.

```bash
# (optional)
# build dependency
cd ../fcs-endpoint-validator-core
mvn clean package
cd ../fcs-endpoint-validator-cli/

# build CLI tool
mvn clean package
```

### Logging

Configuration for logging can be found in [`src/main/resources/log4j2.xml`](src/main/resources/log4j2.xml).

To log more details, e.g. parsed configuration, you can update the log level for `eu.clarin.sru.fcs.validator.cli`.

Due to how log capturing works in the validator, you may need to set `additivity="true"`. But if you are not interested in captured log entries for tests then you can ignore this. _(NOTE: currently the CLI tool does not otherwise process log entries so this paragraph can be ignored.)_

## How to Run

Show help:

```bash
java -jar target/endpoint-validator-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -h
```

Run validation:

```bash
# run validation using configuration file "endpoints.yml", dump results to stdout
java -jar target/endpoint-validator-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -i endpoints.yml

# write results to output file "results.tsv"
java -jar target/endpoint-validator-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -i endpoints.yml -o results.tsv
```

## Formats

### Input / Endpoint Configuration

Endpoints and configurations are stored in a YAML file. It has the following format:

```yml
# fcs test profile for all endpoints
# one of CLARIN_FCS_1_0, CLARIN_FCS_2_0, AGGREGATOR_MIN_FCS, LEX_FCS, CLARIN_FCS_LEGACY or null for auto-detection
profile: null

performProbeRequests: true|false  # perform initial probe request to check if endpoint responds, by default "true"
strictMode: true|false|null  # perform tests in strict mode
connectTimeout: -1|0|1000|null  # connection timeout in milliseconds
socketTimeout: -1|0|1000|null  # socket/search timeout in milliseconds

# required list of endpoints
endpoints:
- url: https://example.de/lcc
  # besides the "url", all the other fields are optional and by default "null"
  # BUT if values are provided in the global scope above, then those will be taken,
  # otherwise endpoint configuration will override them
  profile: null  # fcs test profile
  searchTerm: "Apfel"|null  # search term for tests, some tests will be skipped if not provided
  resourcePids: "res1,http://example.de/lcc#res2"|"some-res-id"|null  # list of resource PIDs for tests, should be a comma separated string
  strictMode: true|false|null  # perform tests in strict mode
  connectTimeout: -1|0|1000|null  # connection timeout in milliseconds
  socketTimeout: -1|0|1000|null  # socket/search timeout in milliseconds
```

Defaults are:

```yml
profile: null
performProbeRequests: true
strictMode: true
connectTimeout: null
socketTimeout: null
endpoints: []  # is empty by default!

# and for a single endpoint
- url: ...  # this field is required, must be provided!
  profile: null
  searchTerm: null
  resourcePids: null
  strictMode: null
  connectTimeout: null
  socketTimeout: null
```

A minimal configuration file can look like this:

```yml
endpoints:
  - url: https://example.de/lcc
```

### Output

Output will be written as TSV (tab-separated-values) list. Using the `-o`/`--results` parameter you can specify an output file, otherwise the standard output will be used.

Example output:

```tsv
URL	Success	Failure	Warning	Skipped	Tests
https://example.de/fcs	16	4	0	0	[explain]yyxyy|[scan]yy|[searchRetrieve]xyyxyyyyyyyyx
https://example.de/fcs2	28	1	0	0	[explain]yyyyyy|[scan]yy|[searchRetrieve]yyyxyyyyyyyyyyyyyyyyy
```

The last column "Tests" is a flat overview over each test result, grouped by category (e.g., "[explain]") with single test results encoded to `y = SUCCESS`, `x = FAILURE`, `w = WARNING`, and `- = SKIPPED` (`? = UNKNWON` should not appear).
