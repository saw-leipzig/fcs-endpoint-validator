package eu.clarin.sru.fcs.tester;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FCSTestLifecycleMethodExecutionExceptionHandler implements LifecycleMethodExecutionExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(FCSTestLifecycleMethodExecutionExceptionHandler.class);

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        // String id = context.getUniqueId();
        String method = throwable.getStackTrace()[0].getMethodName();
        logger.warn("Exception raised in {} @BeforeAll {}()", context.getDisplayName(), method, throwable);
    }
}