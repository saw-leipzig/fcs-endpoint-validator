package eu.clarin.sru.fcs.validator;

import java.util.HashMap;
import java.util.Map;

public class FCSEndpointValidationRequest {

    private Map<String, Object> properties;

    private int connectTimeout = FCSTestHttpClientFactory.DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeout = FCSTestHttpClientFactory.DEFAULT_SOCKET_TIMEOUT;
    private boolean performProbeRequest = true;

    private FCSEndpointValidatorProgressListener progressListener = null;

    private FCSTestProfile profile = null;
    private boolean strictMode = FCSTestContext.DEFAULT_STRICT_MODE;
    private int indentResponse = FCSTestContext.DEFAULT_INDENT_RESPONSE;
    private String baseURI;

    private String userSearchTerm;

    // ----------------------------------------------------------------------

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public boolean isPerformProbeRequest() {
        return performProbeRequest;
    }

    public void setPerformProbeRequest(boolean performProbeRequest) {
        this.performProbeRequest = performProbeRequest;
    }

    // ----------------------------------------------------------------------

    public FCSEndpointValidatorProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(FCSEndpointValidatorProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public FCSTestProfile getFCSTestProfile() {
        return profile;
    }

    public void setFCSTestProfile(FCSTestProfile profile) {
        this.profile = profile;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public int getIndentResponse() {
        return indentResponse;
    }

    public void setIndentResponse(int indentResponse) {
        this.indentResponse = indentResponse;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public String getUserSearchTerm() {
        return userSearchTerm;
    }

    public void setUserSearchTerm(String userSearchTerm) {
        this.userSearchTerm = userSearchTerm;
    }

    // ----------------------------------------------------------------------

    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return getProperty(key, null);
    }

    public Object getProperty(String key, Object defaultValue) {
        if (properties != null) {
            return properties.get(key);
        } else {
            return defaultValue;
        }
    }

    public boolean hasProperty(String key) {
        if (properties != null) {
            return properties.containsKey(key);
        } else {
            return false;
        }
    }

    public Object removeProperty(String key) {
        if (properties != null) {
            return properties.remove(key);
        }
        return null;
    }

}