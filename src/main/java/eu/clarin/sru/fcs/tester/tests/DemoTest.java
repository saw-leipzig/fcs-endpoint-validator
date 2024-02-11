package eu.clarin.sru.fcs.tester.tests;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DemoTest {
    protected static final Logger logger = LoggerFactory.getLogger(DemoTest.class);

    @Test
    void doSomething() {
        logger.info("do something ...");

        org.apache.logging.log4j.LogManager.getLogger("eu.clarin.sru.Test").info("test");
    }
}
