package eu.clarin.sru.fcs.validator.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.validator.FCSEndpointValidator;

@Service
public class FCSEndpointValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidatorService.class);

    protected final FCSEndpointValidatorProperties properties;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Path localPathResults;

    // ----------------------------------------------------------------------

    protected FCSEndpointValidatorService(FCSEndpointValidatorProperties properties) {
        this.properties = properties;

        if (this.properties.isEnabledValidationResultsSaving()) {
            localPathResults = this.properties.getValidationResultsFolder().toPath();

            logger.info("Check if path {} for results exists ...", localPathResults);
            if (!Files.exists(localPathResults)) {
                try {
                    logger.info("Try creating path {} for results", localPathResults);
                    Files.createDirectories(localPathResults);
                } catch (IOException e) {
                    logger.error("Unable to create results output folder!", e);
                    throw new RuntimeException("Unable to create FCS endpoint validation data folder!");
                }
            }

            cleanupOldResults();
        } else {
            localPathResults = null;
        }
    }

    // ----------------------------------------------------------------------

    public void cleanupOldResults() {
        if (this.properties.getMaxLifetimeOfValidationResults() != null) {
            logger.debug("Run cleanup of stale results (max lifetime = {}) ...",
                    this.properties.getMaxLifetimeOfValidationResults());
            Instant oldest = Instant.now().minus(this.properties.getMaxLifetimeOfValidationResults().toSeconds(),
                    ChronoUnit.SECONDS);
            logger.debug("Threshold for deletion: {}", oldest);

            try (DirectoryStream<Path> paths = Files.newDirectoryStream(localPathResults)) {
                for (Path resultPath : paths) {
                    Instant fileTime = Files.getLastModifiedTime(resultPath).toInstant();
                    if (fileTime.isBefore(oldest)) {
                        logger.info("Clean up too old result '{}', age: {}", resultPath, fileTime);
                        Files.delete(resultPath);
                    }
                }
            } catch (IOException e) {
                logger.warn("Unable to iterate over result files!", e);
            }
        }
    }

    // ----------------------------------------------------------------------

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

    public boolean canStoreFCSEndpointValidationResults() {
        return this.properties.isEnabledValidationResultsSaving();
    }

    public String storeFCSEndpointValidationResult(FCSEndpointValidationResult result) {
        if (!this.properties.isEnabledValidationResultsSaving()) {
            return null;
        }

        final String resultId = UUID.randomUUID().toString();

        byte[] bytes = FCSEndpointValidationResult.serialize(result, resultId);
        if (bytes == null) {
            return null;
        }

        File fo = localPathResults.resolve(resultId).toFile();
        try (FileOutputStream fos = new FileOutputStream(fo)) {
            fos.write(bytes);
        } catch (IOException e) {
            logger.error("Error writing endpoint validation results", e);
            return null;
        }

        return resultId;
    }

    public FCSEndpointValidationResult loadFCSEndpointValidationResult(String resultId) {
        if (!this.properties.isEnabledValidationResultsSaving()) {
            return null;
        }

        File fo = localPathResults.resolve(resultId).toFile();
        if (!fo.exists()) {
            return null;
        }

        byte[] bytes = null;
        try (FileInputStream fis = new FileInputStream(fo)) {
            bytes = fis.readAllBytes();
        } catch (IOException e) {
            logger.error("Error reading endpoint validation results", e);
            return null;
        }

        return FCSEndpointValidationResult.deserialize(bytes, resultId);
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
