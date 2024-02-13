package eu.clarin.sru.fcs.tester;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public class HttpRequestResponseInfo {
    private HttpRequest request;
    private HttpResponse response;
    private byte[] responseBytes;

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public void setResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
    }

}