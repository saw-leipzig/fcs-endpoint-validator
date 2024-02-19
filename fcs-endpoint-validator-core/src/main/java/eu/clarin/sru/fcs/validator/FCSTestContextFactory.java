package eu.clarin.sru.fcs.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;

public class FCSTestContextFactory {

    // properties for each FCSTestContext
    private Map<String, Object> properties;
    private FCSTestProfile profile;
    private String baseURI;
    private boolean strictMode = FCSTestContext.DEFAULT_STRICT_MODE;
    private int indentResponse = FCSTestContext.DEFAULT_INDENT_RESPONSE;
    private CloseableHttpClient httpClient;

    private String userSearchTerm;

    // ----------------------------------------------------------------------

    public static FCSTestContextFactory newInstance() {
        return new FCSTestContextFactory();
    }

    // ----------------------------------------------------------------------

    public FCSTestProfile getFCSTestProfile() {
        return profile;
    }

    public void setFCSTestProfile(FCSTestProfile profile) {
        this.profile = profile;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
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

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // ----------------------------------------------------------------------

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

    public void removeProperty(String key) {
        if (properties != null) {
            properties.remove(key);
        }
    }

    // ----------------------------------------------------------------------

    public FCSTestContext newFCSTestContext() {
        if (profile == null) {
            throw new NullPointerException("profile == null");
        }
        if (baseURI == null) {
            throw new NullPointerException("baseURI == null");
        }

        final CloseableHttpClient testHttpClient;
        if (httpClient != null) {
            testHttpClient = httpClient;
        } else {
            // create default http client
            // NOTE: we do not cache it here if not explicitly set by user
            testHttpClient = FCSTestHttpClientFactory.getInstance().newClient();
        }

        final FCSTestContext context = new FCSTestContext(profile, baseURI, strictMode, testHttpClient, userSearchTerm,
                indentResponse);
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                context.setProperty(property.getKey(), property.getValue());
            }
        }
        return context;
    }

}
