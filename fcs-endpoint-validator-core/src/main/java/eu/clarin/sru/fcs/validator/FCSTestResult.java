package eu.clarin.sru.fcs.validator;

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.IncompleteExecutionException;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.TestAbortedWithWarningException;

public class FCSTestResult implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(FCSTestResult.class);

    private String uniqueId;
    private String category;
    private String name;
    private String expected;
    private List<LogEvent> logs;
    private List<HttpRequestResponseInfo> httpRequestResponses;
    private TestExecutionResult.Status status = null;
    private Throwable throwable = null;
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
        this.skipReason = skipReason;

        if (testExecutionResult != null) {
            status = testExecutionResult.getStatus();
            testExecutionResult.getThrowable().ifPresent(t -> {
                this.throwable = t;
            });
        }
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

    public TestExecutionResult.Status getTestExecutionResultStatus() {
        return status;
    }

    public Throwable getTestExecutionResultThrowable() {
        return throwable;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public Throwable getException() {
        if (throwable != null && !(throwable instanceof IncompleteExecutionException)) {
            return throwable;
        }
        return null;
    }

    public String getMessage() {
        // message why skipped or failed (aborted, warning, error, ...)
        if (skipReason != null) {
            return skipReason;
        }
        if (throwable != null) {
            return throwable.getMessage();
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
        if (status == null) {
            // see skipReason
            return FCSTestResultStatus.SKIPPED;
        }
        switch (status) {
            case SUCCESSFUL:
                return FCSTestResultStatus.SUCCESSFUL;
            case FAILED:
                return FCSTestResultStatus.FAILED;
            case ABORTED:
                if (throwable != null) {
                    if (throwable instanceof TestAbortedException
                            && throwable.getMessage().startsWith("Assumption failed")) {
                        return FCSTestResultStatus.SKIPPED;
                    } else if (throwable instanceof TestAbortedWithWarningException) {
                        return FCSTestResultStatus.WARNING;
                    }
                }

        }
        logger.warn("Unknown condition for TestExecutionResult! status = {} throwable = {}", status, throwable);
        return null;
    }

    public boolean isSuccess() {
        return (status != null) ? status == TestExecutionResult.Status.SUCCESSFUL : false;
    }

    public boolean isFailure() {
        return (status != null) ? status == TestExecutionResult.Status.FAILED : false;
    }

    public boolean isWarning() {
        if (status == null) {
            return false;
        }
        if (status == TestExecutionResult.Status.ABORTED && throwable != null) {
            if (throwable instanceof TestAbortedWithWarningException) {
                return true;
            }
        }
        return false;
    }

    public boolean isSkipped() {
        if (status == null) {
            // skipped due to @Disabled or similar
            return true;
        }
        if (status == TestExecutionResult.Status.ABORTED && throwable != null) {
            if (throwable instanceof TestAbortedException && throwable.getMessage().startsWith("Assumption failed")) {
                return true;
            }
        }
        return false;
    }

}
