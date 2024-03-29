package eu.clarin.sru.fcs.validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FCSEndpointValidationResponse implements Serializable {

    private static final long serialVersionUID = 2024_03_04L;

    private final FCSEndpointValidationRequest request;

    private final Map<String, FCSTestResult> results;

    // ----------------------------------------------------------------------

    public FCSEndpointValidationResponse(FCSEndpointValidationRequest request, Map<String, FCSTestResult> results) {
        this.request = request;
        this.results = results;
    }

    public FCSEndpointValidationRequest getRequest() {
        return request;
    }

    public Map<String, FCSTestResult> getResults() {
        return results;
    }

    public List<FCSTestResult> getResultsList() {
        return new ArrayList<>(results.values());
    }

    // ----------------------------------------------------------------------

    public long getCountSuccess() {
        return results.values().stream().filter(r -> r.isSuccess()).count();
    }

    public long getCountWarning() {
        return results.values().stream().filter(r -> r.isWarning()).count();
    }

    public long getCountFailure() {
        return results.values().stream().filter(r -> r.isFailure()).count();
    }

    public long getCountSkipped() {
        return results.values().stream().filter(r -> r.isSkipped()).count();
    }

}
