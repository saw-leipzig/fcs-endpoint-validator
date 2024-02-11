package eu.clarin.sru.fcs.tester;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FCSTestExecutionListener implements TestExecutionListener {
    protected static final Logger logger = LoggerFactory.getLogger(FCSTestExecutionListener.class);

    protected static String LOGCAPTURING_APPENDER_NAME = LogCapturingAppender.class.getName();

    protected LogCapturingAppender appender;

    protected Map<String, FCSTestResult> results = new HashMap<>();

    // ----------------------------------------------------------------------

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.getType() == Type.TEST) {
            logger.info("test started: {}", testIdentifier.getUniqueId());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.getType() == Type.TEST) {
            List<LogEvent> testLogs = gatherLogs(testIdentifier);
            String name = testIdentifier.getUniqueId();
            FCSTestResult result = new FCSTestResult(name, testIdentifier.getDisplayName(), testLogs,
                    testExecutionResult, null);
            results.put(name, result);
            logger.info("test finished: {} {}", testIdentifier.getUniqueId(), testExecutionResult);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.getType() == Type.TEST) {
            List<LogEvent> testLogs = gatherLogs(testIdentifier);
            String name = testIdentifier.getUniqueId();
            FCSTestResult result = new FCSTestResult(name, testIdentifier.getDisplayName(), testLogs, null, reason);
            results.put(name, result);
            logger.info("test skipped: {} {}", testIdentifier.getUniqueId(), reason);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        logger.debug("Add log capturing appender");
        org.apache.logging.log4j.core.Logger sruLogger = ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                .getLogger("eu.clarin.sru"));
        sruLogger.setAdditive(true);
        // sruLogger.setLevel(Level.ALL);

        appender = new LogCapturingAppender(LOGCAPTURING_APPENDER_NAME, null,
                PatternLayout.createDefaultLayout(), true, null);
        appender.start();
        sruLogger.addAppender(appender);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        logger.debug("Remove log capturing appender");
        org.apache.logging.log4j.core.Logger sruLogger = ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                .getLogger("eu.clarin.sru"));
        if (appender != null) {
            appender.stop();
            sruLogger.removeAppender(appender);
            appender = null;
        }
    }

    // ----------------------------------------------------------------------

    public Map<String, FCSTestResult> getResults() {
        return results;
    }

    private List<LogEvent> gatherLogs(TestIdentifier testIdentifier) {
        String name = testIdentifier.getUniqueId();
        Long id = Thread.currentThread().getId();
        logger.debug("Get logs for {} in thread {}", name, id);
        List<LogEvent> testLogs = appender.getLogsAndClear(id);
        return Collections.unmodifiableList((testLogs != null) ? testLogs : Collections.emptyList());
    }
}