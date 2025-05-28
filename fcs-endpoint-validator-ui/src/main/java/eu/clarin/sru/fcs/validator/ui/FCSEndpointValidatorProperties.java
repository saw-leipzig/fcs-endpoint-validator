package eu.clarin.sru.fcs.validator.ui;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@ConfigurationProperties("fcsvalidator")
@Validated
public class FCSEndpointValidatorProperties {

    private File validationResultsFolder = new File("results");

    private boolean enabledValidationResultsSaving = false;

    @DurationUnit(ChronoUnit.DAYS)
    private Duration maxLifetimeOfValidationResults = Duration.ofDays(90);

    @NotNull
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration minDelayBetweenValidationRequests = Duration.ofSeconds(5);

    private boolean enableMatomoTrackingCalls = false;

    // ----------------------------------------------------------------------

    public File getValidationResultsFolder() {
        return validationResultsFolder;
    }

    public void setValidationResultsFolder(File validationResultsFolder) {
        this.validationResultsFolder = validationResultsFolder;
    }

    public boolean isEnabledValidationResultsSaving() {
        return enabledValidationResultsSaving;
    }

    public void setEnabledValidationResultsSaving(boolean enabledValidationResultsSaving) {
        this.enabledValidationResultsSaving = enabledValidationResultsSaving;
    }

    public Duration getMaxLifetimeOfValidationResults() {
        return maxLifetimeOfValidationResults;
    }

    public void setMaxLifetimeOfValidationResults(Duration maxLifetimeOfValidationResults) {
        this.maxLifetimeOfValidationResults = maxLifetimeOfValidationResults;
    }

    public Duration getMinDelayBetweenValidationRequests() {
        return minDelayBetweenValidationRequests;
    }

    public void setMinDelayBetweenValidationRequests(Duration minDelayBetweenValidationRequests) {
        this.minDelayBetweenValidationRequests = minDelayBetweenValidationRequests;
    }

    public boolean isEnableMatomoTrackingCalls() {
        return enableMatomoTrackingCalls;
    }

    public void setEnableMatomoTrackingCalls(boolean enableMatomoTrackingCalls) {
        this.enableMatomoTrackingCalls = enableMatomoTrackingCalls;
    }

}
