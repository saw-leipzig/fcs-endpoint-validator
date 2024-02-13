package eu.clarin.sru.fcs.tester;

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LogCapturingAppender extends AbstractAppender implements Recorder<LogEvent> {
    protected static final String DEFAULT_APPENDER_NAME = LogCapturingAppender.class.getName();
    protected static final String PACKAGE_SRUFCS_TESTER = "eu.clarin.sru.fcs.tester";
    protected static final String PACKAGE_SRUFCS_LIB = "eu.clarin.sru";

    protected final GenericRecorder<LogEvent> recorder = new GenericRecorder<LogEvent>();

    // ----------------------------------------------------------------------

    public LogCapturingAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent event) {
        // we ignore logs from ourselves
        if (event.getLoggerName().startsWith(PACKAGE_SRUFCS_TESTER + ".")) {
            return;
        }
        // and only want to capture events from the SRU/FCS libs (?)
        if (!event.getLoggerName().startsWith(PACKAGE_SRUFCS_LIB + ".")) {
            return;
        }

        recorder.addRecord(event.toImmutable());
    }

    // ----------------------------------------------------------------------

    public static LogCapturingAppender installAppender(LogCapturingAppender appender) {
        // https://logging.apache.org/log4j/2.x/manual/customconfig.html#programmatically-modifying-the-current-configuration-after-initi

        final Level level = Level.ALL;
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        boolean hasLoggerAlreadyConfigured = false;
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            if (loggerConfig.getName().equals(PACKAGE_SRUFCS_LIB)) {
                // check if we have the logger (we want to capture) already defined
                hasLoggerAlreadyConfigured = true;
            }
        }

        if (appender == null) {
            final PatternLayout layout = PatternLayout.createDefaultLayout(config);
            appender = new LogCapturingAppender(DEFAULT_APPENDER_NAME, null, layout, true, null);
        }

        appender.start();
        config.addAppender(appender);

        if (!hasLoggerAlreadyConfigured) {
            // add new logger to set our appender to get the logs
            final LoggerConfig loggerConfig = LoggerConfig.newBuilder()
                    .withAdditivity(false)
                    .withLevel(level)
                    .withLoggerName(PACKAGE_SRUFCS_LIB)
                    .withIncludeLocation("true")
                    .withRefs(new AppenderRef[] { AppenderRef.createAppenderRef(appender.getName(), null, null) })
                    .withConfig(config)
                    .build();
            loggerConfig.addAppender(appender, null, null);
            config.addLogger(PACKAGE_SRUFCS_LIB, loggerConfig);
            ctx.updateLoggers();

            // check for child loggers
            for (final LoggerConfig childLoggerConfig : config.getLoggers().values()) {
                if (childLoggerConfig.getName().equals(PACKAGE_SRUFCS_TESTER)
                        || childLoggerConfig.getName().startsWith(PACKAGE_SRUFCS_TESTER + ".")) {
                    continue;
                }
                // TODO: this does not yet work as we want -- no console output
                // do we need to add the parents (and skip over PACKAGE_SRUFCS_LIB?)
                if (childLoggerConfig.getName().startsWith(PACKAGE_SRUFCS_LIB + ".")) {
                    // we need to increase the level, otherwise nothing can't be captured on certain
                    // levels -- this also means that any other defined logger will have its level
                    // increase too (--> much more output in console/logfile)
                    childLoggerConfig.setLevel(level);
                    // childLoggerConfig.setAdditive(true);
                    // System.out.println(childLoggerConfig.getName() + " .... child");
                }
            }
        } else {
            // check if we have the logger (we want to capture) already defined
            for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
                if (loggerConfig.getName().equals(PACKAGE_SRUFCS_TESTER)
                        || loggerConfig.getName().startsWith(PACKAGE_SRUFCS_TESTER + ".")) {
                    continue;
                }
                if (loggerConfig.getName().equals(PACKAGE_SRUFCS_LIB)) {
                    // we need to increase the level, otherwise nothing can't be captured on certain
                    // levels this also means that any other defined logger will have its level
                    // increase too (--> much more output in console/logfile)
                    loggerConfig.setLevel(level);
                    // loggerConfig.setAdditive(true);
                    loggerConfig.addAppender(appender, level, null);

                    // System.out.println("\nFound logger with same name ..." +
                    // loggerConfig.getName() + "\n");
                    // final Level level2 = loggerConfig.getLevel();
                    // for (final Map.Entry<String,Appender> oldAppender :
                    // loggerConfig.getParent().getAppenders().entrySet()) {
                    // loggerConfig.addAppender(oldAppender.getValue(), level2, null);
                    // }
                }
            }
        }

        return appender;
    }

    public static void removeAppender(LogCapturingAppender appender) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        if (appender != null) {
            appender.stop();
        }

        final String name = (appender != null) ? appender.getName() : DEFAULT_APPENDER_NAME;
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.removeAppender(name);
        }

        ctx.updateLoggers();
    }

    // ----------------------------------------------------------------------

    @Override
    public void addRecord(LogEvent record) {
        recorder.addRecord(record);
    }

    @Override
    public List<LogEvent> getRecords() {
        return recorder.getRecords();
    }

    @Override
    public boolean hasRecords() {
        return recorder.hasRecords();
    }

    @Override
    public List<LogEvent> removeRecords() {
        return recorder.removeRecords();
    }

}