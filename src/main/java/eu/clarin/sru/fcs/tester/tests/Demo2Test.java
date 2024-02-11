package eu.clarin.sru.fcs.tester.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.tester.FCSTestContext;

@Disabled
public final class Demo2Test extends AbstractFCSTest {
    protected static final Logger logger = LoggerFactory.getLogger(Demo2Test.class);

    @Test
    void doSomething() {
        logger.info("do something ...");

        org.apache.logging.log4j.LogManager.getLogger("eu.clarin.sru.Test").info("ab");
        org.apache.logging.log4j.LogManager.getLogger("eu.clarin.ignore.Test").info("efg");
    }

    @Test
    void doSomeFCSStuff(FCSTestContext context) {
        org.apache.logging.log4j.Logger sruLogger = org.apache.logging.log4j.LogManager.getLogger("eu.clarin.sru.Test");
        sruLogger.debug("context: {}", context);
        sruLogger.debug("req: {}", context.createExplainRequest());
    }
}
