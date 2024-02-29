package eu.clarin.sru.fcs.validator.ui;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.Refill;

@Service
public class FCSEndpointValidationRequestIPThrottlerService {
    private static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidationRequestIPThrottlerService.class);

    // TODO: make configurable
    public static final int THROTTLE_SECONDS = 5;

    protected static final FCSEndpointValidationRequestIPThrottlerService instance = new FCSEndpointValidationRequestIPThrottlerService();

    protected final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------------

    protected FCSEndpointValidationRequestIPThrottlerService() {
    }

    public static FCSEndpointValidationRequestIPThrottlerService getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------------

    public Bucket resolveBucket(String ipAddress) {
        return cache.computeIfAbsent(ipAddress, this::newBucket);
    }

    private Bucket newBucket(String ipAddress) {
        final Bandwidth limit = Bandwidth.classic(1, Refill.intervally(1, Duration.ofSeconds(THROTTLE_SECONDS)));
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
