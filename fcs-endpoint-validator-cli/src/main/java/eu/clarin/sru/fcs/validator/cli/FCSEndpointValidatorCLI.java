package eu.clarin.sru.fcs.validator.cli;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.validator.FCSEndpointValidator;
import eu.clarin.sru.fcs.validator.FCSTestResult;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class FCSEndpointValidatorCLI {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidatorCLI.class);

    public static void main(String[] args) throws IOException, SRUClientException {

        OptionParser parser = new OptionParser();
        OptionSpec<Void> argHelp = parser
                .acceptsAll(asList("?", "h", "help"), "Show the help")
                .forHelp();
        OptionSpec<File> argEndpoints = parser
                .acceptsAll(asList("i", "endpoints"), "Configuration file with list of endpoints for validation")
                .withRequiredArg()
                .required()
                .ofType(File.class);
        OptionSpec<File> argOutput = parser
                .acceptsAll(asList("o", "results"), "Output file with validation results for each endpoint")
                .withRequiredArg()
                .ofType(File.class);

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException ex) {
            System.err.println(ex.getLocalizedMessage());
            System.exit(1);
            return;
        }

        if (options == null || options.has(argHelp)) {
            try {
                parser.printHelpOn(System.err);
            } catch (IOException ex) {
                System.err.println(ex.getLocalizedMessage());
            }
            System.exit(1);
            return;
        }

        logger.info("Start FCS Endpoint Validator!");
        logger.debug("CLI Options: {}", options.asMap());

        File endpointFile = options.valueOf(argEndpoints);
        Configuration configuration = loadConfiguration(endpointFile);
        if (configuration == null) {
            System.exit(1);
            return;
        }
        logger.debug("Configuration: {}", configuration);

        Map<Configuration.Endpoint, FCSEndpointValidationResponse> results = runValidation(configuration);
        logger.debug("Validation results: {}", results);

        if (options.has(argOutput)) {
            try (PrintStream ps = new PrintStream(options.valueOf(argOutput))) {
                writeResults(results, ps);
            } catch (Exception ex) {
                System.err.println(ex.getLocalizedMessage());
            }
        } else {
            writeResults(results, null);
        }

        logger.info("done");
    }

    private static Configuration loadConfiguration(File configFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        Configuration configuration = null;
        try {
            configuration = mapper.readValue(configFile, Configuration.class);
        } catch (InvalidFormatException ex) {
            // check for valid enum value in configuration!
            System.err.println(ex.getLocalizedMessage());
        } catch (MismatchedInputException ex) {
            // check for required "url" parameter in configuration!
            System.err.println(ex.getLocalizedMessage());
        }
        // @formatter:off
        // com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException -- wrong property form
        // @formatter:on

        return configuration;
    }

    private static Map<Configuration.Endpoint, FCSEndpointValidationResponse> runValidation(
            Configuration configuration) {
        Map<Configuration.Endpoint, FCSEndpointValidationResponse> results = new HashMap<>();

        for (Configuration.Endpoint endpoint : configuration.getEndpoints()) {
            final FCSEndpointValidationRequest request = new FCSEndpointValidationRequest();
            request.setBaseURI(endpoint.getUrl());
            request.setUserSearchTerm(endpoint.getSearchTerm());
            Optional.ofNullable(endpoint.getResourcePids())
                    .ifPresent((pids) -> request.setUserResourcePids(pids.split(",")));
            request.setPerformProbeRequest(configuration.isPerformProbeRequests());
            Optional.ofNullable(endpoint.getProfile())
                    .or(() -> Optional.ofNullable(configuration.getProfile()))
                    .ifPresent(profile -> request.setFCSTestProfile(profile));
            Optional.ofNullable(endpoint.getConnectTimeout())
                    .or(() -> Optional.ofNullable(configuration.getConnectTimeout()))
                    .ifPresent(timeout -> request.setConnectTimeout(timeout));
            Optional.ofNullable(endpoint.getSocketTimeout())
                    .or(() -> Optional.ofNullable(configuration.getSocketTimeout()))
                    .ifPresent(timeout -> request.setSocketTimeout(timeout));
            request.setStrictMode(Optional.ofNullable(endpoint.isStrictMode()).orElse(configuration.isStrictMode()));

            logger.info("Run validation on {}", request);
            try {
                final FCSEndpointValidationResponse response = FCSEndpointValidator.runValidation(request);
                results.put(endpoint, response);
            } catch (IOException | SRUClientException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    private static void writeResults(Map<Configuration.Endpoint, FCSEndpointValidationResponse> results,
            PrintStream ps) {
        if (ps == null) {
            System.out.println("URL\tSuccess\tFailure\tWarning\tSkipped\tTests");
            ps = System.out;
        }

        for (FCSEndpointValidationResponse result : results.values()) {
            ps.println(String.format("%s\t%s\t%s\t%s\t%s\t%s",
                    result.getRequest().getBaseURI(),
                    result.getCountSuccess(),
                    result.getCountFailure(),
                    result.getCountWarning(),
                    result.getCountSkipped(),
                    buildTestResultPattern(result)));
        }
    }

    private static String buildTestResultPattern(FCSEndpointValidationResponse response) {
        List<FCSTestResult> tests = response.getResultsList();
        tests.sort((a, b) -> {
            int val = a.getCategory().compareTo(b.getCategory());
            return (val != 0) ? val : a.getName().compareTo(b.getName());
        });

        // group tests by category, make single letter for each test result and combine
        return tests.stream().collect(Collectors.groupingBy(e -> e.getCategory().split(" ")[0]))
                .entrySet().stream().map(e -> String.format("[%s]%s", e.getKey(),
                        e.getValue().stream().map(FCSTestResult::getStatus).map(s -> {
                            switch (s) {
                                case SUCCESSFUL:
                                    return "y";
                                case FAILED:
                                    return "x";
                                case WARNING:
                                    return "w";
                                case SKIPPED:
                                    return "-";
                                default:
                                    return "?";
                            }
                        }).collect(Collectors.joining())))
                .collect(Collectors.joining("|"));
    }
}
