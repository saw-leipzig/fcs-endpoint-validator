package eu.clarin.sru.fcs.tester;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.platform.engine.TestExecutionResult;

public class FCSTestResult {
    private String uniqueId;
    private String name;
    private List<LogEvent> logs;
    private List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> httpRequestResponses;
    private TestExecutionResult testExecutionResult = null;
    private String skipReason = null;

    // ----------------------------------------------------------------------

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs,
            List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> httpRequestResponses,
            TestExecutionResult testExecutionResult, String skipReason) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.logs = logs;
        this.httpRequestResponses = httpRequestResponses;
        this.testExecutionResult = testExecutionResult;
        this.skipReason = skipReason;
    }

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs,
            List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> httpRequestResponses,
            TestExecutionResult testExecutionResult) {
        this(uniqueId, name, logs, httpRequestResponses, testExecutionResult, null);
    }

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs,
            List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> httpRequestResponses,
            String skipReason) {
        this(uniqueId, name, logs, httpRequestResponses, null, skipReason);
    }

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs, TestExecutionResult testExecutionResult,
            String skipReason) {
        this(uniqueId, name, logs, null, testExecutionResult, skipReason);
    }

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs, String skipReason) {
        this(uniqueId, name, logs, null, null, skipReason);
    }

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs, TestExecutionResult testExecutionResult) {
        this(uniqueId, name, logs, null, testExecutionResult, null);
    }

    // ----------------------------------------------------------------------

    public String getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public List<LogEvent> getLogs() {
        return logs;
    }

    public List<HttpRequestResponseRecordingInterceptor.HttpRequestResponseInfo> getHttpRequestResponseInfos() {
        return httpRequestResponses;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public String getSkipReason() {
        return skipReason;
    }
}
