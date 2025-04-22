package eu.clarin.sru.fcs.validator.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.EstimationProbe;

@Service
public class FCSEndpointValidationRequestIPThrottlerService {
    protected final FCSEndpointValidatorProperties properties;

    protected final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------------

    protected FCSEndpointValidationRequestIPThrottlerService(FCSEndpointValidatorProperties properties) {
        this.properties = properties;
    }

    // ----------------------------------------------------------------------

    public Bucket resolveBucket(String ipAddress) {
        return cache.computeIfAbsent(ipAddress, this::newBucket);
    }

    private Bucket newBucket(String ipAddress) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(1)
                .refillIntervally(1, this.properties.getMinDelayBetweenValidationRequests())
                .initialTokens(1).build();
        return Bucket.builder().addLimit(limit).build();
    }

    // ----------------------------------------------------------------------

    public boolean canConsume(String ipAddress) {
        Bucket tokenBucket = resolveBucket(ipAddress);
        EstimationProbe probe = tokenBucket.estimateAbilityToConsume(1);
        return probe.canBeConsumed();
    }

    public long waitTimeUntilConsumable(String ipAddress) {
        Bucket tokenBucket = resolveBucket(ipAddress);
        EstimationProbe probe = tokenBucket.estimateAbilityToConsume(1);
        return (probe.canBeConsumed()) ? 0 : (probe.getNanosToWaitForRefill() / 1_000_000_000);
    }

    public boolean tryConsume(String ipAddress, Runnable validAction) {
        Bucket tokenBucket = resolveBucket(ipAddress);
        boolean isConsumed = tokenBucket.tryConsume(1);
        if (isConsumed) {
            validAction.run();
        }
        return isConsumed;
    }

    public void tryConsumeOrElse(String ipAddress, Runnable validAction, Runnable invalidAction) {
        Bucket tokenBucket = resolveBucket(ipAddress);
        if (tokenBucket.tryConsume(1)) {
            validAction.run();
        } else {
            invalidAction.run();
        }
    }

}
