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
    public final Map<Long, List<LogEvent>> logs = new HashMap<>();

    public LogCapturingAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent event) {
        // we only want to capture those events
        if (!event.getLoggerName().startsWith("eu.clarin.sru.")) {
            return;
        }
        // and also ignore logs from ourselves
        if (event.getLoggerName().startsWith("eu.clarin.sru.fcs.tester.")) {
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

}