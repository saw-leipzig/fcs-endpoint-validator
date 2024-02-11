package eu.clarin.sru.fcs.tester;

import java.util.Map;

public class FCSTestContextFactory {
    // singleton
    private static FCSTestContextFactory instance;

    // properties for each FCSTestContext
    private Map<String, Object> properties;
    private FCSTestProfile profile;
    private String baseURI;
    private boolean strictMode = true;
    private int connectTimeout = FCSTestContext.DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeout = FCSTestContext.DEFAULT_SOCKET_TIMEOUT;

    // ----------------------------------------------------------------------

    private static FCSTestContextFactory newInstance() {
        return new FCSTestContextFactory();
    }

    public static synchronized FCSTestContextFactory getInstance() {
        if (instance == null) {
            instance = newInstance();
        }
        return instance;
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

    // ----------------------------------------------------------------------

    public FCSTestContext newFCSTestContext() {
        if (profile == null) {
            throw new NullPointerException("profile == null");
        }
        if (baseURI == null) {
            throw new NullPointerException("baseURI == null");
        }

        FCSTestContext context = new FCSTestContext(profile, baseURI, strictMode, connectTimeout, socketTimeout);
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                context.setProperty(property.getKey(), property.getValue());
            }
        }
        return context;
    }

}
