package eu.clarin.sru.fcs.validator.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.validator.FCSTestProfile;

public class Configuration {

    public static class Endpoint {
        private String url;
        private FCSTestProfile profile;
        private String searchTerm;
        private String resourcePids;
        private Boolean strictMode = null;
        private Integer connectTimeout = null;
        private Integer socketTimeout = null;

        // ------------------------------------------------------------------

        @JsonCreator
        public Endpoint(@JsonProperty(value = "url", required = true) String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public FCSTestProfile getProfile() {
            return profile;
        }

        public void setProfile(FCSTestProfile profile) {
            this.profile = profile;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public String getResourcePids() {
            return resourcePids;
        }

        public void setResourcePids(String resourcePids) {
            this.resourcePids = resourcePids;
        }

        public Boolean isStrictMode() {
            return strictMode;
        }

        public void setStrictMode(Boolean strictMode) {
            this.strictMode = strictMode;
        }

        public Integer getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Integer getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(Integer socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        @Override
        public String toString() {
            return "Endpoint [url=" + url + ", profile=" + profile + ", searchTerm=" + searchTerm + ", resourcePids="
                    + resourcePids + ", strictMode=" + strictMode + ", connectTimeout=" + connectTimeout
                    + ", socketTimeout=" + socketTimeout + "]";
        }

    }

    // ----------------------------------------------------------------------

    private FCSTestProfile profile;
    private boolean performProbeRequests = true;
    private boolean strictMode = true;
    private Integer connectTimeout = null;
    private Integer socketTimeout = null;
    private List<Endpoint> endpoints = new ArrayList<>();

    // ----------------------------------------------------------------------

    public Configuration() {
    }

    public List<Endpoint> getEndpoints() {
        if (endpoints == null) {
            return Collections.emptyList();
        }
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public boolean isPerformProbeRequests() {
        return performProbeRequests;
    }

    public void setPerformProbeRequests(boolean performProbeRequests) {
        this.performProbeRequests = performProbeRequests;
    }

    public FCSTestProfile getProfile() {
        return profile;
    }

    public void setProfile(FCSTestProfile profile) {
        this.profile = profile;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    @Override
    public String toString() {
        return "Configuration [profile=" + profile + ", performProbeRequests=" + performProbeRequests + ", strictMode="
                + strictMode + ", connectTimeout=" + connectTimeout + ", socketTimeout=" + socketTimeout
                + ", endpoints=" + endpoints + "]";
    }

}
