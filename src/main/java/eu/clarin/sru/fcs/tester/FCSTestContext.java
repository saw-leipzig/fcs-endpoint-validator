package eu.clarin.sru.fcs.tester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientConfig;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescriptionParser;
import eu.clarin.sru.client.fcs.ClarinFCSRecordDataParser;
import eu.clarin.sru.client.fcs.DataViewParser;
import eu.clarin.sru.client.fcs.DataViewParserAdvanced;
import eu.clarin.sru.client.fcs.DataViewParserGenericString;
import eu.clarin.sru.client.fcs.DataViewParserHits;
import eu.clarin.sru.client.fcs.DataViewParserKWIC;
import eu.clarin.sru.client.fcs.LegacyClarinFCSRecordDataParser;

public class FCSTestContext {

    public static final String PROPERTY_USER_AGENT = FCSTestContext.class.getName() + ":USER_AGENT";

    public static final String DEFAULT_USER_AGENT = "FCSEndpointTester/1.0.0";
    public static final int DEFAULT_CONNECT_TIMEOUT = -1;
    public static final int DEFAULT_SOCKET_TIMEOUT = -1;

    private Map<String, Object> properties;
    private final FCSTestProfile profile;
    private final String baseURI;
    private final boolean strictMode;
    private final int connectTimeout;
    private final int socketTimeout;

    // ----------------------------------------------------------------------

    public FCSTestContext(FCSTestProfile profile, String baseURI, boolean strictMode, int connectTimeout,
            int socketTimeout) {
        if (profile == null) {
            throw new NullPointerException("profile == null");
        }
        if (baseURI == null) {
            throw new NullPointerException("baseURI == null");
        }

        this.profile = profile;
        this.baseURI = baseURI;
        this.strictMode = strictMode;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
    }

    public FCSTestProfile getFCSTestProfile() {
        return profile;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    // ----------------------------------------------------------------------

    public SRUExplainRequest createExplainRequest() {
        SRUExplainRequest request = new SRUExplainRequest(baseURI.toString());
        request.setStrictMode(strictMode);
        return request;
    }

    public SRUScanRequest createScanRequest() {
        SRUScanRequest request = new SRUScanRequest(baseURI.toString());
        request.setStrictMode(strictMode);
        return request;
    }

    public SRUSearchRetrieveRequest createSearchRetrieveRequest() {
        SRUSearchRetrieveRequest request = new SRUSearchRetrieveRequest(baseURI.toString());
        request.setStrictMode(strictMode);
        return request;
    }

    // ----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    protected SRUClientConfig buildSRUClientConfig() {
        final SRUClientConfig.Builder builder = new SRUClientConfig.Builder();

        // SRU version
        SRUVersion version = SRUVersion.VERSION_1_2;
        switch (profile) {
            case CLARIN_FCS_2_0:
                version = SRUVersion.VERSION_2_0;
                break;
            case CLARIN_FCS_1_0:
                /* $FALL-THROUGH$ */
            case CLARIN_FCS_LEGACY:
                version = SRUVersion.VERSION_1_2;
                break;
        }

        builder.setDefaultVersion(version)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setRequestAuthenticator(null);

        boolean legacySupport = (profile == FCSTestProfile.CLARIN_FCS_LEGACY
                || (!strictMode && profile == FCSTestProfile.CLARIN_FCS_1_0));
        boolean fullLegacyCompatMode = (profile == FCSTestProfile.CLARIN_FCS_LEGACY);

        // record and data view parsers
        // see eu.clarin.sru.client.fcs.ClarinFCSClientBuilder
        final List<DataViewParser> parsers = new ArrayList<>();
        parsers.add(new DataViewParserHits());
        parsers.add(new DataViewParserAdvanced());
        parsers.add(new DataViewParserGenericString());
        if (legacySupport) {
            parsers.add(new DataViewParserKWIC(fullLegacyCompatMode));
        }
        builder.addRecordDataParser(new ClarinFCSRecordDataParser(parsers));
        if (legacySupport) {
            builder.addRecordDataParser(new LegacyClarinFCSRecordDataParser(parsers, fullLegacyCompatMode));
        }
        if (profile != FCSTestProfile.CLARIN_FCS_LEGACY) {
            builder.addExtraResponseDataParser(new ClarinFCSEndpointDescriptionParser());
        }

        return builder.build();
    }

    public SRUClient getClient() {
        return new SRUClient(buildSRUClientConfig());
    }

    // ----------------------------------------------------------------------

    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        if (properties != null) {
            return properties.get(key);
        } else {
            return null;
        }
    }

    public boolean hasProperty(String key) {
        if (properties != null) {
            return properties.containsKey(key);
        } else {
            return false;
        }
    }

}
