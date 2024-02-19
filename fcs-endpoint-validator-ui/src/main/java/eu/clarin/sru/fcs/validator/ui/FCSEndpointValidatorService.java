package eu.clarin.sru.fcs.validator.ui;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.fcs.validator.FCSEndpointValidator;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;

@Service
public class FCSEndpointValidatorService {
    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidatorService.class);

    protected static final FCSEndpointValidatorService instance = new FCSEndpointValidatorService();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ----------------------------------------------------------------------

    protected FCSEndpointValidatorService() {
    }

    public static FCSEndpointValidatorService getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------------

    // @Async
    public CompletableFuture<FCSEndpointValidationResponse> evalute(FCSEndpointValidationRequest request) {
        CompletableFuture<FCSEndpointValidationResponse> completableFuture = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                FCSEndpointValidationResponse response = FCSEndpointValidator.runValidation(request);
                completableFuture.complete(response);
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
