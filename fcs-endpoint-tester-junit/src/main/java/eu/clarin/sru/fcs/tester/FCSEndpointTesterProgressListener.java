package eu.clarin.sru.fcs.tester;

public interface FCSEndpointTesterProgressListener {

    public default void onStarted(FCSEndpointValidationRequest request) {
    }

    public default void onFinished(FCSEndpointValidationResponse response) {
    }

    public default void onError(Throwable t) {
    }

    // ----------------------------------------------------------------------

    public default void onProgressMessage(String message) {
    }

    // ----------------------------------------------------------------------

    public default void onTestStarted(String testId, String testName) {
    }

    public default void onTestFinished(String testId, String testName, FCSTestResult result) {
    }

}