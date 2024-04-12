package eu.clarin.sru.fcs.validator.cli;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.core.pattern.NameAbbreviator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.validator.FCSEndpointValidator;
import eu.clarin.sru.fcs.validator.FCSTestProfile;
import eu.clarin.sru.fcs.validator.FCSTestResult;

public class FCSEndpointValidatorCLI {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidatorCLI.class);
    protected static final NameAbbreviator logNameConverter = NameAbbreviator.getAbbreviator("1.");

    public static void main(String[] args) throws IOException, SRUClientException {
        logger.info("Start FCS Endpoint Validator!");

        final FCSEndpointValidationRequest request = new FCSEndpointValidationRequest();
        request.setBaseURI("https://fcs.data.saw-leipzig.de/lcc");
        request.setUserSearchTerm("test");
        request.setFCSTestProfile(FCSTestProfile.CLARIN_FCS_2_0);

        final FCSEndpointValidationResponse response = FCSEndpointValidator.runValidation(request);
        dumpLogs(response.getResults());

        logger.info("done");
    }

    private static void dumpLogs(Map<String, FCSTestResult> results) {
        results.entrySet().forEach(e -> {
            if (e.getValue().getLogs().isEmpty()) {
                logger.info("No logs for {}.", e.getKey());
            } else {
                logger.info("Logs for {}:", e.getKey());
                e.getValue().getLogs().forEach(l -> logger.info("  - [{}][{}] {}", l.getLevel(),
                        formatClassName(l.getLoggerName()), l.getMessage()));
            }
        });
    }

    protected static String formatClassName(String classname) {
        StringBuilder buf = new StringBuilder();
        logNameConverter.abbreviate(classname, buf);
        return buf.toString();
    }
}
