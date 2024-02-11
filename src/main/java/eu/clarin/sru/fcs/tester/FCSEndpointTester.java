package eu.clarin.sru.fcs.tester;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

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

public class FCSEndpointTester {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointTester.class);

    public static void main(String[] args) {
        logger.info("Hello World!");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("eu.clarin.sru.fcs.tester.tests"))
                .filters(includeClassNamePatterns(".*Test"))
                .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LogCapturingTestExecutionListener testExecListener = new LogCapturingTestExecutionListener();

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

        logger.info("logs: {}", testExecListener.getLogs());
        testExecListener.getLogs().entrySet().forEach(e -> {
            logger.info("Logs for {}:", e.getKey());
            e.getValue().forEach(l -> logger.info("  - {}", l.getMessage()));
        });

        logger.info("done");
    }
}
