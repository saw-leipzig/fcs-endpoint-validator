package eu.clarin.sru.fcs.validator;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.IncompleteExecutionException;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.TestAbortedWithWarningException;

public class FCSTestResult {
    private static final Logger logger = LoggerFactory.getLogger(FCSTestResult.class);

    private String uniqueId;
    private String category;
    private String name;
    private String expected;
    private List<LogEvent> logs;
    private List<HttpRequestResponseInfo> httpRequestResponses;
    private TestExecutionResult testExecutionResult = null;
    private String skipReason = null;

    // ----------------------------------------------------------------------

    public FCSTestResult(String uniqueId, String category, String name, String expected, List<LogEvent> logs,
            List<HttpRequestResponseInfo> httpRequestResponses,
            TestExecutionResult testExecutionResult, String skipReason) {
        this.uniqueId = uniqueId;
        this.category = category;
        this.name = name;
        this.expected = expected;
        this.logs = logs;
        this.httpRequestResponses = httpRequestResponses;
        this.testExecutionResult = testExecutionResult;
        this.skipReason = skipReason;
    }

    public FCSTestResult(String uniqueId, String category, String name, String expected, List<LogEvent> logs,
            List<HttpRequestResponseInfo> httpRequestResponses, TestExecutionResult testExecutionResult) {
        this(uniqueId, category, name, expected, logs, httpRequestResponses, testExecutionResult, null);
    }

    public FCSTestResult(String uniqueId, String category, String name, String expected, List<LogEvent> logs,
            List<HttpRequestResponseInfo> httpRequestResponses, String skipReason) {
        this(uniqueId, category, name, expected, logs, httpRequestResponses, null, skipReason);
    }

    public FCSTestResult(String uniqueId, String category, String name, String expected, List<LogEvent> logs,
            TestExecutionResult testExecutionResult, String skipReason) {
        this(uniqueId, category, name, expected, logs, null, testExecutionResult, skipReason);
    }

    public FCSTestResult(String uniqueId, String category, String name, String expected, List<LogEvent> logs,
            String skipReason) {
        this(uniqueId, category, name, expected, logs, null, null, skipReason);
    }

    public FCSTestResult(String uniqueId, String category, String name, String expected, List<LogEvent> logs,
            TestExecutionResult testExecutionResult) {
        this(uniqueId, category, name, expected, logs, null, testExecutionResult, null);
    }

    // ----------------------------------------------------------------------

    public String getUniqueId() {
        return uniqueId;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getExpected() {
        return expected;
    }

    public List<LogEvent> getLogs() {
        return logs;
    }

    public List<HttpRequestResponseInfo> getHttpRequestResponseInfos() {
        return httpRequestResponses;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public Throwable getException() {
        if (testExecutionResult.getThrowable().isPresent()) {
            Throwable t = testExecutionResult.getThrowable().get();
            if (!(t instanceof IncompleteExecutionException)) {
                return t;
            }
        }
        return null;
    }

    public String getMessage() {
        // message why skipped or failed (aborted, warning, error, ...)
        if (skipReason != null) {
            return skipReason;
        }
        if (testExecutionResult.getThrowable().isPresent()) {
            return testExecutionResult.getThrowable().get().getMessage();
        }
        return null;
    }

    // ----------------------------------------------------------------------

    public static enum FCSTestResultStatus {
        SUCCESSFUL,
        FAILED,
        WARNING,
        SKIPPED;
    }

    public FCSTestResultStatus getStatus() {
        if (testExecutionResult == null) {
            // see skipReason
            return FCSTestResultStatus.SKIPPED;
        }
        switch (testExecutionResult.getStatus()) {
            case SUCCESSFUL:
                return FCSTestResultStatus.SUCCESSFUL;
            case FAILED:
                return FCSTestResultStatus.FAILED;
            case ABORTED:
                if (testExecutionResult.getThrowable().isPresent()) {
                    Throwable ex = testExecutionResult.getThrowable().get();
                    if (ex instanceof TestAbortedException && ex.getMessage().startsWith("Assumption failed")) {
                        return FCSTestResultStatus.SKIPPED;
                    } else if (ex instanceof TestAbortedWithWarningException) {
                        return FCSTestResultStatus.WARNING;
                    }
                }

        }
        logger.warn("Unknown condition for TestExecutionResult! result = {}", testExecutionResult);
        return null;
    }

    public boolean isSuccess() {
        return (testExecutionResult != null) ? testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL
                : false;
    }

    public boolean isFailure() {
        return (testExecutionResult != null) ? testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED
                : false;
    }

    public boolean isWarning() {
        if (testExecutionResult == null) {
            return false;
        }
        if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
            Throwable ex = testExecutionResult.getThrowable().get();
            if (ex instanceof TestAbortedWithWarningException) {
                return true;
            }
        }
        return false;
    }

    public boolean isSkipped() {
        if (testExecutionResult == null) {
            // skipped due to @Disabled or similar
            return true;
        }
        if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED
                && testExecutionResult.getThrowable().isPresent()) {
            Throwable ex = testExecutionResult.getThrowable().get();
            if (ex instanceof TestAbortedException && ex.getMessage().startsWith("Assumption failed")) {
                return true;
            }
        }
        return false;
    }

}