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
import org.springframework.stereotype.Service;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.fcs.tester.FCSEndpointTester;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.tester.FCSTestResult;


@Service
public class FCSEndpointTesterService {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointTesterService.class);

    protected static final FCSEndpointTesterService instance = new FCSEndpointTesterService();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ----------------------------------------------------------------------

    protected FCSEndpointTesterService() {
    }

    public static FCSEndpointTesterService getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------------

    // @Async
    public CompletableFuture<List<FCSTestResult>> evalute(FCSEndpointValidationRequest request) {
        CompletableFuture<List<FCSTestResult>> completableFuture = new CompletableFuture<>();
        
        executor.submit(() -> {
            try {
                Map<String, FCSTestResult> results = FCSEndpointTester.runValidation(request);
                completableFuture.complete(Collections.unmodifiableList(new ArrayList<>(results.values())));
            } catch (IOException | SRUClientException e) {
                logger.error("Error running validation", e);
                completableFuture.obtrudeException(e);
            }
        });
        
        return completableFuture; 
    }

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
