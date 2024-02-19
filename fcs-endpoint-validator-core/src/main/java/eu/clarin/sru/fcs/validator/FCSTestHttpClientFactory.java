package eu.clarin.sru.fcs.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class FCSTestHttpClientFactory {
    public static final String PROPERTY_USER_AGENT = FCSTestHttpClientFactory.class.getName() + ":USER_AGENT";
    public static final String PROPERTY_REQUEST_INTERCEPTOR = FCSTestHttpClientFactory.class.getName()
            + ":REQUEST_INTERCEPTOR";
    public static final String PROPERTY_RESPONSE_INTERCEPTOR = FCSTestHttpClientFactory.class.getName()
            + ":RESPONSE_INTERCEPTOR";

    public static final String DEFAULT_USER_AGENT = "FCSEndpointValidator/1.0.0";
    public static final int DEFAULT_CONNECT_TIMEOUT = -1;
    public static final int DEFAULT_SOCKET_TIMEOUT = -1;

    // singleton
    private static FCSTestHttpClientFactory instance;

    private Map<String, Object> properties;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    // ----------------------------------------------------------------------

    private static FCSTestHttpClientFactory newInstance() {
        return new FCSTestHttpClientFactory();
    }

    public static synchronized FCSTestHttpClientFactory getInstance() {
        if (instance == null) {
            instance = newInstance();
        }
        return instance;
    }

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

    public CloseableHttpClient newClient() {
        String userAgent = (String) getProperty(PROPERTY_USER_AGENT, DEFAULT_USER_AGENT);

        HttpRequestInterceptor requestInterceptor = null;
        if (hasProperty(PROPERTY_REQUEST_INTERCEPTOR)) {
            Object value = getProperty(PROPERTY_REQUEST_INTERCEPTOR);
            if (value instanceof HttpRequestInterceptor) {
                requestInterceptor = (HttpRequestInterceptor) value;
            }
        }
        HttpResponseInterceptor responseInterceptor = null;
        if (hasProperty(PROPERTY_RESPONSE_INTERCEPTOR)) {
            Object value = getProperty(PROPERTY_RESPONSE_INTERCEPTOR);
            if (value instanceof HttpResponseInterceptor) {
                responseInterceptor = (HttpResponseInterceptor) value;
            }
        }

        return createHttpClient(userAgent, connectTimeout, socketTimeout, requestInterceptor, responseInterceptor);
    }

    // ----------------------------------------------------------------------

    protected static CloseableHttpClient createHttpClient(String userAgent, int connectTimeout, int socketTimeout) {
        return createHttpClient(userAgent, connectTimeout, socketTimeout, null, null);
    }

    protected static CloseableHttpClient createHttpClient(String userAgent, int connectTimeout, int socketTimeout,
            HttpRequestInterceptor requestInterceptor, HttpResponseInterceptor responseInterceptor) {
        final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setDefaultMaxPerRoute(8);
        manager.setMaxTotal(128);

        final SocketConfig socketConfig = SocketConfig.custom()
                .setSoReuseAddress(true)
                .setSoLinger(0)
                .build();

        final RequestConfig requestConfig = RequestConfig.custom()
                .setAuthenticationEnabled(false)
                .setRedirectsEnabled(true)
                .setMaxRedirects(4)
                .setCircularRedirectsAllowed(false)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(0) /* infinite */
                .build();

        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setUserAgent(userAgent)
                .setConnectionManager(manager)
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(requestConfig)
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy());

        // add optional interceptors to capture requests / responses
        if (requestInterceptor != null) {
            clientBuilder.addInterceptorLast(requestInterceptor);
        }
        if (responseInterceptor != null) {
            clientBuilder.addInterceptorLast(responseInterceptor);
        }

        return clientBuilder.build();
    }

}
