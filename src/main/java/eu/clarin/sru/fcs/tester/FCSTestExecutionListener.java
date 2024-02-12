package eu.clarin.sru.fcs.tester;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FCSTestExecutionListener implements TestExecutionListener {
    protected static final Logger logger = LoggerFactory.getLogger(FCSTestExecutionListener.class);

    protected static final String LOGCAPTURING_APPENDER_NAME = LogCapturingAppender.class.getName();
    protected static final String LOGCAPTURING_LOGGER_NAME = "eu.clarin.sru";

    protected LogCapturingAppender appender;
    protected HttpRequestResponseRecordingInterceptor httpRequestResponseRecordingInterceptor;

    protected Map<String, FCSTestResult> results = new HashMap<>();

    public FCSTestExecutionListener(HttpRequestResponseRecordingInterceptor httpRequestResponseRecordingInterceptor) {
        this.httpRequestResponseRecordingInterceptor = httpRequestResponseRecordingInterceptor;
    }

    public FCSTestExecutionListener() {
        this(null);
    }

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
            List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> httpRequestResponseInfos = gatherHttpRequestResponseInfos(
                    testIdentifier);
            String name = testIdentifier.getUniqueId();
            FCSTestResult result = new FCSTestResult(name, testIdentifier.getDisplayName(), testLogs,
                    httpRequestResponseInfos, testExecutionResult);
            results.put(name, result);
            logger.info("test finished: {} {}", testIdentifier.getUniqueId(), testExecutionResult);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.getType() == Type.TEST) {
            List<LogEvent> testLogs = gatherLogs(testIdentifier);
            List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> httpRequestResponseInfos = gatherHttpRequestResponseInfos(
                    testIdentifier);
            String name = testIdentifier.getUniqueId();
            FCSTestResult result = new FCSTestResult(name, testIdentifier.getDisplayName(), testLogs,
                    httpRequestResponseInfos, reason);
            results.put(name, result);
            logger.info("test skipped: {} {}", testIdentifier.getUniqueId(), reason);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        logger.debug("Add log capturing appender");

        // see
        // https://logging.apache.org/log4j/2.x/manual/customconfig.html#programmatically-modifying-the-current-configuration-after-initi
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final PatternLayout layout = PatternLayout.createDefaultLayout(config);

        appender = new LogCapturingAppender(LOGCAPTURING_APPENDER_NAME, null, layout, true, null);
        appender.start();

        config.addAppender(appender);

        // add custom appender to get logs
        AppenderRef ref = AppenderRef.createAppenderRef(LOGCAPTURING_APPENDER_NAME, null, null);
        AppenderRef[] refs = new AppenderRef[] { ref };
        @SuppressWarnings("deprecation")
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.ALL, LOGCAPTURING_LOGGER_NAME, "true", refs,
                null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger(LOGCAPTURING_LOGGER_NAME, loggerConfig);

        ctx.updateLoggers();
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

    private List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> gatherHttpRequestResponseInfos(
            TestIdentifier testIdentifier) {
        if (httpRequestResponseRecordingInterceptor == null) {
            return Collections.unmodifiableList(Collections.emptyList());
        }

        String name = testIdentifier.getUniqueId();
        Long id = Thread.currentThread().getId();
        logger.debug("Get http request/response info for {} in thread {}", name, id);
        List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> testHttpRequestResponseInfos = httpRequestResponseRecordingInterceptor
                .getHttpRequestResponseInfosAndClear(id);
        return Collections.unmodifiableList(
                (testHttpRequestResponseInfos != null) ? testHttpRequestResponseInfos : Collections.emptyList());
    }
}