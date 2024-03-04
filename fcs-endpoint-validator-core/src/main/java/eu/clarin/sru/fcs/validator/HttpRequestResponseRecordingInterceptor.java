package eu.clarin.sru.fcs.validator;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestResponseRecordingInterceptor extends GenericRecorder<HttpRequestResponseInfo>
        implements HttpRequestInterceptor, HttpResponseInterceptor {
    protected static final Logger logger = LoggerFactory.getLogger(HttpRequestResponseRecordingInterceptor.class);

    // ----------------------------------------------------------------------

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        final HttpRequestResponseInfo httpReqRespInfo = new HttpRequestResponseInfo();
        httpReqRespInfo.setRequest(request);

        addRecord(httpReqRespInfo);

        logger.debug("sending request {}", request.toString());
        // logger.debug("sending request to {}", getRequestUri(request));
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        // check for last (current) element
        HttpRequestResponseInfo httpReqRespInfo = getLastRecord();
        if (httpReqRespInfo == null) {
            // only the case if we only record responses, so we start with an empty list
            httpReqRespInfo = new HttpRequestResponseInfo();
            addRecord(httpReqRespInfo);
        } else if (httpReqRespInfo.getResponse() != null) {
            // if there is already a response then we create a new container
            httpReqRespInfo = new HttpRequestResponseInfo();
            addRecord(httpReqRespInfo);
        }

        httpReqRespInfo.setResponse(response);

        logger.debug("got response: {}", response.getStatusLine());

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            // make a copy so we can read from it but others can still use it afterwards
            HttpEntity copy = copy(entity);
            response.setEntity(copy);

            // and the copy we can read how often we want since it is now in memory
            try (InputStream is = copy.getContent()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is.transferTo(baos);
                final byte[] content = baos.toByteArray();
                httpReqRespInfo.setResponseBytes(content);
                logger.debug("response length: {}", content.length);
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
                                : ContentType.DEFAULT_BINARY);

        try (InputStream is = entity.getContent()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            copy.setBinary(baos.toByteArray()); // is.readAllBytes()
        } catch (UnsupportedOperationException | IOException e) {
            logger.debug("Error trying to copy HTTP entity input stream", e);
        }

        return copy.build();
    }

}