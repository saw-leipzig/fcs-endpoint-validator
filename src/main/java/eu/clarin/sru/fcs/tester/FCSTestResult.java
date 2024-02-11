package eu.clarin.sru.fcs.tester;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.platform.engine.TestExecutionResult;

public class FCSTestResult {
    private String uniqueId;
    private String name;
    private List<LogEvent> logs;
    private TestExecutionResult testExecutionResult = null;
    private String skipReason = null;

    public FCSTestResult(String uniqueId, String name, List<LogEvent> logs, TestExecutionResult testExecutionResult,
            String skipReason) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.logs = logs;
        this.testExecutionResult = testExecutionResult;
        this.skipReason = skipReason;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public List<LogEvent> getLogs() {
        return logs;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public String getSkipReason() {
        return skipReason;
    }
}
