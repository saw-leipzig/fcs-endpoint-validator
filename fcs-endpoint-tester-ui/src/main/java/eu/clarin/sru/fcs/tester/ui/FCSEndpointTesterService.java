package eu.clarin.sru.fcs.tester.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.fcs.tester.FCSEndpointTester;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.tester.FCSTestResult;


public class FCSEndpointTesterService {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointTesterService.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ----------------------------------------------------------------------

    protected FCSEndpointTesterService() {
    }

    public static FCSEndpointTesterService getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------------


    // ----------------------------------------------------------------------

    protected void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            logger.debug("thread pool terminated");
        } catch (InterruptedException e) {
            /* IGNORE */
        }
    }

}
