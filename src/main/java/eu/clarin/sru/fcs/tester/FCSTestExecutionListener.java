package eu.clarin.sru.fcs.tester;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.tester.tests.AbstractFCSTest.Expected;

public class FCSTestExecutionListener implements TestExecutionListener {
    protected static final Logger logger = LoggerFactory.getLogger(FCSTestExecutionListener.class);

    protected static final String LOGCAPTURING_APPENDER_NAME = LogCapturingAppender.class.getName();
    protected static final String LOGCAPTURING_LOGGER_NAME = "eu.clarin.sru";
    protected static final String LOGCAPTURING_LOGGER_IGNORE = "eu.clarin.sru.fcs.tester";

    protected LogCapturingAppender appender;
    protected HttpRequestResponseRecordingInterceptor httpRequestResponseRecordingInterceptor;

    protected Map<String, FCSTestResult> results = new LinkedHashMap<>();

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
            String expected = getExpectedAnnotationValue(testIdentifier);
            FCSTestResult result = new FCSTestResult(name, testIdentifier.getDisplayName(), expected, testLogs,
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
            String expected = getExpectedAnnotationValue(testIdentifier);
            FCSTestResult result = new FCSTestResult(name, testIdentifier.getDisplayName(), expected, testLogs,
                    httpRequestResponseInfos, reason);
            results.put(name, result);
            logger.info("test skipped: {} {}", testIdentifier.getUniqueId(), reason);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        logger.debug("Add log capturing appender");

        // https://logging.apache.org/log4j/2.x/manual/customconfig.html#programmatically-modifying-the-current-configuration-after-initi
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        boolean hasLoggerAlreadyConfigured = false;
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            if (loggerConfig.getName().equals(LOGCAPTURING_LOGGER_NAME)) {
                // check if we have the logger (we want to capture) already defined
                hasLoggerAlreadyConfigured = true;
            }
        }

        final PatternLayout layout = PatternLayout.createDefaultLayout(config);

        appender = new LogCapturingAppender(LOGCAPTURING_APPENDER_NAME, null, layout, true, null);
        appender.start();

        config.addAppender(appender);

        if (!hasLoggerAlreadyConfigured) {
            // add new logger to set our appender to get the logs
            AppenderRef ref = AppenderRef.createAppenderRef(LOGCAPTURING_APPENDER_NAME, null, null);
            AppenderRef[] refs = new AppenderRef[] { ref };
            @SuppressWarnings("deprecation")
            LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.ALL, LOGCAPTURING_LOGGER_NAME, "true",
                    refs, null, config, null);
            loggerConfig.addAppender(appender, null, null);
            config.addLogger(LOGCAPTURING_LOGGER_NAME, loggerConfig);

            ctx.updateLoggers();

            // check for child loggers
            for (final LoggerConfig childLoggerConfig : config.getLoggers().values()) {
                if (childLoggerConfig.getName().equals(LOGCAPTURING_LOGGER_IGNORE)
                        || childLoggerConfig.getName().startsWith(LOGCAPTURING_LOGGER_IGNORE + ".")) {
                    continue;
                }
                // TODO: this does not yet work as we want -- no console output
                // do we need to add the parents (and skip over?)
                if (childLoggerConfig.getName().startsWith(LOGCAPTURING_LOGGER_NAME + ".")) {
                    // we need to increase the level, otherwise nothing can't be captured on certain
                    // levels -- this also means that any other defined logger will have its level
                    // increase too (--> much more output in console/logfile)
                    childLoggerConfig.setLevel(Level.ALL);
                    // childLoggerConfig.setAdditive(true);
                    System.out.println(childLoggerConfig.getName() + " .... child");
                }
            }
        } else {
            // check if we have the logger (we want to capture) already defined
            for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
                if (loggerConfig.getName().equals(LOGCAPTURING_LOGGER_IGNORE)
                        || loggerConfig.getName().startsWith(LOGCAPTURING_LOGGER_IGNORE + ".")) {
                    continue;
                }
                if (loggerConfig.getName().equals(LOGCAPTURING_LOGGER_NAME)) {
                    // we need to increase the level, otherwise nothing can't be captured on certain
                    // levels this also means that any other defined logger will have its level
                    // increase too (--> much more output in console/logfile)
                    loggerConfig.setLevel(Level.ALL);
                    // loggerConfig.setAdditive(true);
                    loggerConfig.addAppender(appender, Level.ALL, null);

                    // System.out.println("\nFound logger with same name ..." +
                    // loggerConfig.getName() + "\n");
                    // final Level level = loggerConfig.getLevel();
                    // for (final Map.Entry<String,Appender> oldAppender :
                    // loggerConfig.getParent().getAppenders().entrySet()) {
                    // loggerConfig.addAppender(oldAppender.getValue(), level, null);
                    // }
                }
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        logger.debug("Remove log capturing appender");
        // TODO: rewrite this, too?
        org.apache.logging.log4j.core.Logger sruLogger = ((org.apache.logging.log4j.core.Logger) LogManager
                .getLogger(LOGCAPTURING_LOGGER_NAME));
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

    private String getExpectedAnnotationValue(TestIdentifier testIdentifier) {
        if (!testIdentifier.getSource().isPresent()) {
            return null;
        }
        TestSource source = testIdentifier.getSource().get();
        if (!(source instanceof MethodSource)) {
            return null;
        }
        MethodSource mSource = (MethodSource) source;
        Method method = mSource.getJavaMethod();
        Expected expected = method.getAnnotation(Expected.class);
        if (expected == null) {
            return null;
        }
        return expected.value();
    }

}