package eu.clarin.sru.fcs.tester;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCapturingTestExecutionListener implements TestExecutionListener {
    protected static final Logger logger = LoggerFactory.getLogger(LogCapturingTestExecutionListener.class);

    protected static String LOGCAPTURING_APPENDER_NAME = "LOGCAPTURE_SRUFCS";
    protected LogCapturingAppender appender;
    protected Map<String, Long> testThreadIds = new HashMap<>();

    protected Map<String, List<LogEvent>> logs = new HashMap<>();

    public Map<String, List<LogEvent>> getLogs() {
        return logs;
    }

    private void gatherLogs(TestIdentifier testIdentifier) {
        String name = testIdentifier.getUniqueId();
        Long id = Thread.currentThread().getId();
        logger.debug("Get logs for {} in thread {}", name, id);
        logs.put(name, appender.logs.remove(id));
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.getType() == Type.TEST) {
            logger.info("test started: {}", testIdentifier.getUniqueId());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.getType() == Type.TEST) {
            gatherLogs(testIdentifier);
            logger.info("test finished: {} {}", testIdentifier.getUniqueId(), testExecutionResult);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.getType() == Type.TEST) {
            gatherLogs(testIdentifier);
            logger.info("test skipped: {} {}", testIdentifier.getUniqueId(), reason);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        logger.debug("Add log capturing appender");
        org.apache.logging.log4j.core.Logger sruLogger = ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                .getLogger("eu.clarin.sru"));
        sruLogger.setAdditive(true);

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

}