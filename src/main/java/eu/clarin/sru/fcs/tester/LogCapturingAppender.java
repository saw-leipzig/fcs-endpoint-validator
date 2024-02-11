package eu.clarin.sru.fcs.tester;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class LogCapturingAppender extends AbstractAppender {
    protected static final String PACKAGE_SRUFCS_TESTER = "eu.clarin.sru.fcs.tester.";
    protected static final String PACKAGE_SRUFCS_LIB = "eu.clarin.sru.";

    protected final Map<Long, List<LogEvent>> logs = new HashMap<>();

    // ----------------------------------------------------------------------

    public LogCapturingAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent event) {
        // we ignore logs from ourselves
        if (event.getLoggerName().startsWith(PACKAGE_SRUFCS_TESTER)) {
            return;
        }
        // and only want to capture events from the SRU/FCS libs (?)
        if (!event.getLoggerName().startsWith(PACKAGE_SRUFCS_LIB)) {
            return;
        }

        Long id = Thread.currentThread().getId();
        synchronized (logs) {
            List<LogEvent> localLogs = logs.get(id);
            if (localLogs == null) {
                localLogs = new ArrayList<>();
                logs.put(id, localLogs);
            }
            localLogs.add(event.toImmutable());
        }
    }

    // ----------------------------------------------------------------------

    public List<LogEvent> getLogs(Long id) {
        synchronized (logs) {
            return logs.get(id);
        }
    }

    public List<LogEvent> getLogs() {
        Long id = Thread.currentThread().getId();
        return getLogs(id);
    }

    public void clearLogs(Long id) {
        synchronized (logs) {
            logs.remove(id);
        }
    }

    public void clearLogs() {
        Long id = Thread.currentThread().getId();
        clearLogs(id);
    }

    public List<LogEvent> getLogsAndClear(Long id) {
        synchronized (logs) {
            return logs.remove(id);
        }
    }

    public List<LogEvent> getLogsAndClear() {
        Long id = Thread.currentThread().getId();
        return getLogsAndClear(id);
    }
}