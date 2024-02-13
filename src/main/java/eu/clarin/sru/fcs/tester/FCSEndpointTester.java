package eu.clarin.sru.fcs.tester;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.core.pattern.NameAbbreviator;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.fcs.utils.ClarinFCSEndpointVersionAutodetector;
import eu.clarin.sru.client.fcs.utils.ClarinFCSEndpointVersionAutodetector.AutodetectedFCSVersion;

public class FCSEndpointTester {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointTester.class);
    protected static final NameAbbreviator logNameConverter = NameAbbreviator.getAbbreviator("1.");

    public static void main(String[] args) throws SRUClientException, IOException {
        logger.info("Start FCS Endpoint Tester!");

        // required user supplied parameters
        final FCSEndpointValidationRequest request = new FCSEndpointValidationRequest();
        request.setBaseURI("https://fcs.data.saw-leipzig.de/lcc");
        request.setUserSearchTerm("test");
        request.setFCSTestProfile(FCSTestProfile.CLARIN_FCS_2_0);

        runValidation(request);

        logger.info("done");
    }

    // ----------------------------------------------------------------------

    public static void runValidation(FCSEndpointValidationRequest request) throws IOException, SRUClientException {
        final boolean parallel = false;
        final boolean debug = true;

        // initial check if endpoint is available
        if (request.isPerformProbeRequest()) {
            // will fail early if endpoint can't be reached!
            performProbeRequest(request.getBaseURI());
            logger.debug("Endpoint at '{}' can be reached.", request.getBaseURI());
        }

        // if not supplied, try to fcs test detect profile
        if (request.getFCSTestProfile() == null) {
            logger.info("No endpoint version supplied, trying to detect ...");
            final FCSTestProfile detectedProfile = detectFCSEndpointVersion(request.getBaseURI());
            request.setFCSTestProfile(detectedProfile);
        }

        // capture request/response pairs
        HttpRequestResponseRecordingInterceptor httpReqRespRecorder = new HttpRequestResponseRecordingInterceptor();
        FCSTestHttpClientFactory httpClientFactory = FCSTestHttpClientFactory.getInstance();
        httpClientFactory.setConnectTimeout(request.getConnectTimeout());
        httpClientFactory.setSocketTimeout(request.getSocketTimeout());
        httpClientFactory.setProperty(FCSTestHttpClientFactory.PROPERTY_REQUEST_INTERCEPTOR, httpReqRespRecorder);
        httpClientFactory.setProperty(FCSTestHttpClientFactory.PROPERTY_RESPONSE_INTERCEPTOR, httpReqRespRecorder);

        // configure test context
        FCSTestContextFactory contextFactory = FCSTestContextFactory.getInstance();
        contextFactory.setFCSTestProfile(request.getFCSTestProfile());
        contextFactory.setStrictMode(request.isStrictMode());
        contextFactory.setBaseURI(request.getBaseURI());
        contextFactory.setUserSearchTerm(request.getUserSearchTerm());
        // we set client here to reuse it for all tests
        contextFactory.setHttpClient(httpClientFactory.newClient());

        // what tests to run
        LauncherDiscoveryRequestBuilder ldRequestBuilder = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("eu.clarin.sru.fcs.tester.tests"))
                .filters(includeClassNamePatterns(".*Test"))

                // enable order based on @Order annotation
                // (might not be guaranteed if running concurrently)
                .configurationParameter("junit.jupiter.testmethod.order.default",
                        "org.junit.jupiter.api.MethodOrderer$OrderAnnotation")
                .configurationParameter("junit.jupiter.testclass.order.default ",
                        "org.junit.jupiter.api.ClassOrderer$OrderAnnotation");

        if (parallel) {
            logger.info("Will perform tests in parallel (by class)!");
            ldRequestBuilder.configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                    .configurationParameter("junit.jupiter.execution.parallel.mode.default", "same_thread")
                    .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent");
        }
        LauncherDiscoveryRequest ldRequest = ldRequestBuilder.build();

        // only for debugging
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        // to capture test logs, results etc.
        FCSTestExecutionListener testExecListener = new FCSTestExecutionListener(httpReqRespRecorder);

        // run tests
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            // Register test execution listeners
            if (debug) {
                launcher.registerTestExecutionListeners(listener);
            }
            // to track test progress and capture logs/http stuff
            launcher.registerTestExecutionListeners(testExecListener);

            // Discover tests and build a test plan
            TestPlan testPlan = launcher.discover(ldRequest);

            // Execute test plan
            launcher.execute(testPlan);
            // Alternatively, execute the request directly
            // launcher.execute(ldRequest);
        }

        if (debug) {
            TestExecutionSummary summary = listener.getSummary();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            summary.printTo(pw);
            Arrays.asList(baos.toString().split("\n")).stream()
                    .filter(l -> !l.isBlank())
                    .forEach(l -> logger.info("{}", l.stripLeading()));
        }

        Map<String, FCSTestResult> results = testExecListener.getResults();
        // TODO: check resource leakage for http request/response stuff?

        // dumpLogs(results);
        writeTestResults(results, false);
    }

    private static FCSTestProfile detectFCSEndpointVersion(String endpointURI) throws SRUClientException {
        final ClarinFCSEndpointVersionAutodetector versionAutodetector = new ClarinFCSEndpointVersionAutodetector();

        AutodetectedFCSVersion version = null;
        try {
            version = versionAutodetector.autodetectVersion(endpointURI);
        } catch (SRUClientException e) {
            logger.error("Error trying to detect endpoint version", e);
            throw new SRUClientException("An error occured while auto-detecting CLARIN-FCS version", e);
        }

        logger.debug("Auto-detected endpoint version = {}", version);

        FCSTestProfile profile = null;
        switch (version) {
            case FCS_LEGACY:
                profile = FCSTestProfile.CLARIN_FCS_LEGACY;
                break;
            case FCS_1_0:
                profile = FCSTestProfile.CLARIN_FCS_1_0;
                break;
            case FCS_2_0:
                profile = FCSTestProfile.CLARIN_FCS_2_0;
                break;
            case UNKNOWN:
                /* $FALL-THROUGH$ */
            default:
                throw new SRUClientException("Unable to auto-detect CLARIN-FCS version!");
        }

        return profile;
    }

    private static void performProbeRequest(final String baseURI)
            throws IOException {
        try {
            logger.debug("performing initial probe request to {}", baseURI);
            final CloseableHttpClient client = FCSTestHttpClientFactory.getInstance().newClient();
            final HttpHead request = new HttpHead(baseURI);
            HttpResponse response = null;
            try {
                response = client.execute(request);
                StatusLine status = response.getStatusLine();
                if (status.getStatusCode() != HttpStatus.SC_OK) {
                    throw new IOException(
                            "Probe request to endpoint returned unexpected HTTP status " + status.getStatusCode());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
                HttpClientUtils.closeQuietly(client);
            }
        } catch (ClientProtocolException e) {
            throw new IOException(e);
        }
    }

    // ----------------------------------------------------------------------

    private static void writeTestResults(Map<String, FCSTestResult> results, boolean hideAborted) {
        logger.info("Endpoint test results:");
        long countFailed = results.values().stream()
                .filter(r -> r.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED).count();
        long countAborted = results.values().stream()
                .filter(r -> r.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED).count();
        long countSuccess = results.values().stream()
                .filter(r -> r.getTestExecutionResult().getStatus() == TestExecutionResult.Status.SUCCESSFUL).count();
        logger.info("  --> Tests: {} ok, {} skipped, {} with error.", countSuccess, countAborted, countFailed);
        logger.info("  --> {}",
                (countFailed == 0) ? "Endpoint performs according to specification." : "Endpoint shows issues!");

        logger.info("Endpoint test result details:");
        results.entrySet().forEach(e -> {
            FCSTestResult result = e.getValue();

            // silently skip
            if (hideAborted && result.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                return;
            }

            final String status;
            switch (result.getTestExecutionResult().getStatus()) {
                case SUCCESSFUL:
                    status = "✔";
                    break;
                case FAILED:
                    status = "✘";
                    break;
                case ABORTED:
                    status = "!";
                    break;
                default:
                    status = "?"; // ✨ // this should never happen
                    break;
            }
            logger.info(" {} >> {} << ({} logs, {} https)", status, result.getName(), result.getLogs().size(),
                    result.getHttpRequestResponseInfos().size());
            switch (result.getTestExecutionResult().getStatus()) {
                case FAILED:
                    logger.info("      * failed, reason: {}",
                            (result.getTestExecutionResult().getThrowable().isPresent())
                                    ? result.getTestExecutionResult().getThrowable().get().getMessage()
                                    : "~~ unknown ~~");
                    logger.info("      * expected: {}", result.getExpected());
                    break;
                case ABORTED:
                    // NOTE: might also be a warning?
                    logger.info("      * skipped, reason: {}", (result.getSkipReason() != null) ? result.getSkipReason()
                            : (result.getTestExecutionResult().getThrowable().isPresent())
                                    ? result.getTestExecutionResult().getThrowable().get().getMessage()
                                    : "~~ unknown ~~");
                    logger.info("      * expected: {}", result.getExpected());
                    break;
                default:
                    break;
            }

        });
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
