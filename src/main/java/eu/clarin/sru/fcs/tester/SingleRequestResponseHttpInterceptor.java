package eu.clarin.sru.fcs.tester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;

public class SingleRequestResponseHttpInterceptor
        implements HttpRequestInterceptor, HttpResponseInterceptor {

    private HttpRequest request;
    private HttpResponse response;
    private byte[] responseBytes;

    // ----------------------------------------------------------------------

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        reset();
        this.request = request;

        FCSEndpointTester.logger.debug("sending request {}", request.toString());
        FCSEndpointTester.logger.debug("sending request to {}", getRequestUri(request));
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        this.response = response;

        FCSEndpointTester.logger.info("got response: {}", response.getStatusLine());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            HttpEntity copy = copy(entity);
            response.setEntity(copy);

            // and the copy we can read how often we want since it is now in memory
            try (InputStream is = copy.getContent()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is.transferTo(baos);
                responseBytes = baos.toByteArray();
                FCSEndpointTester.logger.info("response length: {}", responseBytes.length);
            }
        }
    }

    // ----------------------------------------------------------------------

    public static String getRequestUri(HttpRequest request) {
        if (request == null) {
            return null;
        }
        HttpRequest original = request;
        while (original instanceof HttpRequestWrapper) {
            original = ((HttpRequestWrapper) original).getOriginal();
        }
        return original.getRequestLine().getUri();
    }

    protected static HttpEntity copy(HttpEntity entity) {
        if (entity == null) {
            return null;
        }

        EntityBuilder copy = EntityBuilder.create()
                .setContentEncoding(
                        (entity.getContentEncoding() != null) ? entity.getContentEncoding().getValue()
                                : null)
                .setContentType(
                        (entity.getContentType() != null)
                                ? ContentType.parse(entity.getContentType().getValue())
                                : null);

        try (InputStream is = entity.getContent()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            copy.setBinary(baos.toByteArray()); // is.readAllBytes()
        } catch (UnsupportedOperationException | IOException e) {
            FCSEndpointTester.logger.debug("Error trying to copy HTTP entity input stream", e);
        }

        return copy.build();
    }

    // ----------------------------------------------------------------------

    public void reset() {
        request = null;
        response = null;
        responseBytes = null;
    }

    // ----------------------------------------------------------------------

    public HttpRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

}