package eu.clarin.sru.fcs.tester;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.tester.tests.AbstractFCSTest.Category;
import eu.clarin.sru.fcs.tester.tests.AbstractFCSTest.Expected;

public class FCSTestExecutionListener implements TestExecutionListener {
    protected static final Logger logger = LoggerFactory.getLogger(FCSTestExecutionListener.class);

    protected LogCapturingAppender logRecorder;
    protected HttpRequestResponseRecordingInterceptor httpRequestResponseRecorder;

    protected Map<String, FCSTestResult> results = new LinkedHashMap<>();

    public FCSTestExecutionListener(LogCapturingAppender logRecorder,
            HttpRequestResponseRecordingInterceptor httpRequestResponseRecorder) {
        this.logRecorder = logRecorder;
        this.httpRequestResponseRecorder = httpRequestResponseRecorder;
    }

    public FCSTestExecutionListener(LogCapturingAppender logRecorder) {
        this(logRecorder, null);
    }

    public FCSTestExecutionListener(HttpRequestResponseRecordingInterceptor httpRequestResponseRecorder) {
        this(null, httpRequestResponseRecorder);
    }

    public FCSTestExecutionListener() {
        this(null, null);
    }

    // ----------------------------------------------------------------------

    // TODO: incept BeforeAll to get its requests/responses
    // org.junit.jupiter.api.extension.InvocationInterceptor#interceptBeforeAllMethod

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.getType() == Type.TEST) {
            logger.info("test started: {}", testIdentifier.getUniqueId());

            // clear logs and requests/responses from before (e.g., from @BeforeAll methods)
            String name = testIdentifier.getUniqueId();

            if (logRecorder != null) {
                List<LogEvent> testLogs = logRecorder.removeRecords();
                if (testLogs != null) {
                    logger.debug("Found {} log entries from before current test {}, silently dropping them.",
                            testLogs.size(), name);
                }
            }

            if (httpRequestResponseRecorder != null) {
                List<HttpRequestResponseInfo> testHttpRequestResponseInfos = httpRequestResponseRecorder
                        .removeRecords();
                if (testHttpRequestResponseInfos != null) {
                    logger.debug("Found {} request/response infos from before current test {}, silently dropping them.",
                            testHttpRequestResponseInfos.size(), name);
                }
            }
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.getType() == Type.TEST) {
            // TODO: add exception log entry if Status = FAILED?

            List<LogEvent> testLogs = gatherLogs(testIdentifier);
            List<HttpRequestResponseInfo> testHttps = gatherHttpRequestResponseInfos(testIdentifier);
            String name = testIdentifier.getUniqueId();
            FCSTestResult result = new FCSTestResult(name, getCategoryValue(testIdentifier),
                    testIdentifier.getDisplayName(), getExpectedAnnotationValue(testIdentifier), testLogs, testHttps,
                    testExecutionResult);
            results.put(name, result);

            logger.info("test finished: {} {}", testIdentifier.getUniqueId(), testExecutionResult);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.getType() == Type.TEST) {
            List<LogEvent> testLogs = gatherLogs(testIdentifier);
            List<HttpRequestResponseInfo> testHttps = gatherHttpRequestResponseInfos(testIdentifier);
            String name = testIdentifier.getUniqueId();
            FCSTestResult result = new FCSTestResult(name, getCategoryValue(testIdentifier),
                    testIdentifier.getDisplayName(), getExpectedAnnotationValue(testIdentifier), testLogs, testHttps,
                    reason);
            results.put(name, result);

            logger.info("test skipped: {} {}", testIdentifier.getUniqueId(), reason);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        logger.debug("Add log capturing appender");
        logRecorder = LogCapturingAppender.installAppender(logRecorder);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        logger.debug("Remove log capturing appender");
        LogCapturingAppender.removeAppender(logRecorder);
    }

    // ----------------------------------------------------------------------

    public Map<String, FCSTestResult> getResults() {
        return results;
    }

    private List<LogEvent> gatherLogs(TestIdentifier testIdentifier) {
        if (logRecorder == null) {
            return Collections.emptyList();
        }

        String name = testIdentifier.getUniqueId();
        Long id = Thread.currentThread().getId();
        logger.debug("Get logs for {} in thread {}", name, id);

        List<LogEvent> testLogs = logRecorder.removeRecords();
        return (testLogs != null) ? Collections.unmodifiableList(testLogs) : Collections.emptyList();
    }

    private List<HttpRequestResponseInfo> gatherHttpRequestResponseInfos(
            TestIdentifier testIdentifier) {
        if (httpRequestResponseRecorder == null) {
            return Collections.emptyList();
        }

        String name = testIdentifier.getUniqueId();
        Long id = Thread.currentThread().getId();
        logger.debug("Get http request/response info for {} in thread {}", name, id);

        List<HttpRequestResponseInfo> testHttpRequestResponseInfos = httpRequestResponseRecorder.removeRecords(id);
        return (testHttpRequestResponseInfos != null) ? Collections.unmodifiableList(testHttpRequestResponseInfos)
                : Collections.emptyList();
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

    private String getCategoryValue(TestIdentifier testIdentifier) {
        if (!testIdentifier.getSource().isPresent()) {
            return null;
        }
        TestSource source = testIdentifier.getSource().get();
        if (!(source instanceof MethodSource)) {
            return null;
        }
        MethodSource mSource = (MethodSource) source;
        Class<?> clazz = mSource.getJavaClass();
        Optional<Category> maybeCategory = AnnotationUtils.findAnnotation(clazz, Category.class, false);
        if (!maybeCategory.isPresent()) {
            return null;
        }
        Category category = maybeCategory.get();
        return category.value();
    }

}