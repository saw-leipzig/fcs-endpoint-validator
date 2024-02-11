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
        final String baseURI = "https://fcs.data.saw-leipzig.de/lcc";
        FCSTestProfile profile = null;

        // initial check if endpoint is available
        performProbeRequest(baseURI, FCSTestContext.DEFAULT_USER_AGENT, FCSTestContext.DEFAULT_CONNECT_TIMEOUT,
                FCSTestContext.DEFAULT_SOCKET_TIMEOUT);
        logger.debug("Endpoint at '{}' can be reached.", baseURI);

        // if not supplied, detect profile
        if (profile == null) {
            logger.info("No endpoint version supplied, trying to detect ...");
            profile = detectFCSEndpointVersion(baseURI);
        }

        // configure test context
        FCSTestContextFactory contextFactory = FCSTestContextFactory.getInstance();
        contextFactory.setBaseURI(baseURI);
        contextFactory.setFCSTestProfile(profile);

        // capture request/response pairs
        // TODO: need to add the connection to the FCSTestExecutionListener to add them
        // to the results ...
        SingleRequestResponseHttpInterceptor reqRespCapturer = new SingleRequestResponseHttpInterceptor();
        contextFactory.setProperty(FCSTestContext.PROPERTY_REQUEST_INTERCEPTOR, reqRespCapturer);
        contextFactory.setProperty(FCSTestContext.PROPERTY_RESPONSE_INTERCEPTOR, reqRespCapturer);

        FCSEndpointTester tester = new FCSEndpointTester();
        Map<String, FCSTestResult> results = tester.runTests();

        results.entrySet().forEach(e -> {
            logger.info("Logs for {}:", e.getKey());
            e.getValue().getLogs().forEach(l -> logger.info("  - [{}][{}] {}", l.getLevel(),
                    formatClassName(l.getLoggerName()), l.getMessage()));
        });

        logger.info("done");
    }

    protected Map<String, FCSTestResult> runTests() {
        // what tests to run
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("eu.clarin.sru.fcs.tester.tests"))
                .filters(includeClassNamePatterns(".*Test"))
                .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        // to capture test logs, results etc.
        FCSTestExecutionListener testExecListener = new FCSTestExecutionListener();

        // run tests
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            // Register a listener of your choice
            launcher.registerTestExecutionListeners(listener, testExecListener);
            // Discover tests and build a test plan
            TestPlan testPlan = launcher.discover(request);
            // Execute test plan
            launcher.execute(testPlan);
            // Alternatively, execute the request directly
            // launcher.execute(request);
        }

        TestExecutionSummary summary = listener.getSummary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        summary.printTo(pw);
        Arrays.asList(baos.toString().split("\n")).stream()
                .filter(l -> !l.isBlank())
                .forEach(l -> logger.info("{}", l.stripLeading()));

        return testExecListener.getResults();
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

    private static void performProbeRequest(final String baseURI, String userAgent, int connectTimeout,
            int socketTimeout)
            throws IOException {
        try {
            logger.debug("performing initial probe request to {}", baseURI);
            final CloseableHttpClient client = FCSTestContext.createHttpClient(userAgent, connectTimeout,
                    socketTimeout);
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

    protected static String formatClassName(String classname) {
        StringBuilder buf = new StringBuilder();
        logNameConverter.abbreviate(classname, buf);
        return buf.toString();
    }
}
