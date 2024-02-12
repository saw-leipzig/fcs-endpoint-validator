package eu.clarin.sru.fcs.tester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class HttpRequestResponseRecordingInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
    protected static final Logger logger = LoggerFactory.getLogger(HttpRequestResponseRecordingInterceptor.class);

    public static class HttpRequestResponseInfo {
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

    protected final Map<Long, List<HttpRequestResponseInfo>> httpReqRespInfos = new HashMap<>();

    // ----------------------------------------------------------------------

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        Long id = Thread.currentThread().getId();
        synchronized (httpReqRespInfos) {
            List<HttpRequestResponseInfo> localReqRespInfos = httpReqRespInfos.get(id);
            if (localReqRespInfos == null) {
                localReqRespInfos = new ArrayList<>();
                httpReqRespInfos.put(id, localReqRespInfos);
            }

            final HttpRequestResponseInfo httpReqRespInfo = new HttpRequestResponseInfo();
            httpReqRespInfo.setRequest(request);

            localReqRespInfos.add(httpReqRespInfo);
        }

        logger.debug("sending request {}", request.toString());
        // logger.debug("sending request to {}", getRequestUri(request));
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        HttpRequestResponseInfo httpReqRespInfo;
        Long id = Thread.currentThread().getId();
        synchronized (httpReqRespInfos) {
            List<HttpRequestResponseInfo> localReqRespInfos = httpReqRespInfos.get(id);
            if (localReqRespInfos == null) {
                localReqRespInfos = new ArrayList<>();
                httpReqRespInfos.put(id, localReqRespInfos);
            }

            if (localReqRespInfos.isEmpty()) {
                // only the case if we only record responses, so we start with an empty list
                httpReqRespInfo = new HttpRequestResponseInfo();
                localReqRespInfos.add(httpReqRespInfo);
            } else {
                // check last (current) element
                httpReqRespInfo = localReqRespInfos.get(localReqRespInfos.size() - 1);
                if (httpReqRespInfo.getResponse() != null) {
                    // if there is already a response then we create a new container
                    httpReqRespInfo = new HttpRequestResponseInfo();
                    localReqRespInfos.add(httpReqRespInfo);
                }
            }
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
                                : null);

        try (InputStream is = entity.getContent()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            copy.setBinary(baos.toByteArray()); // is.readAllBytes()
        } catch (UnsupportedOperationException | IOException e) {
            logger.debug("Error trying to copy HTTP entity input stream", e);
        }

        return copy.build();
    }

    // ----------------------------------------------------------------------

    public List<HttpRequestResponseInfo> getHttpRequestResponseInfos(Long id) {
        synchronized (httpReqRespInfos) {
            return httpReqRespInfos.get(id);
        }
    }

    public List<HttpRequestResponseInfo> getHttpRequestResponseInfos() {
        Long id = Thread.currentThread().getId();
        return getHttpRequestResponseInfos(id);
    }

    public void clearHttpRequestResponseInfos(Long id) {
        synchronized (httpReqRespInfos) {
            httpReqRespInfos.remove(id);
        }
    }

    public void clearHttpRequestResponseInfos() {
        Long id = Thread.currentThread().getId();
        clearHttpRequestResponseInfos(id);
    }

    public List<HttpRequestResponseInfo> getHttpRequestResponseInfosAndClear(Long id) {
        synchronized (httpReqRespInfos) {
            return httpReqRespInfos.remove(id);
        }
    }

    public List<HttpRequestResponseInfo> getHttpRequestResponseInfosAndClear() {
        Long id = Thread.currentThread().getId();
        return getHttpRequestResponseInfosAndClear(id);
    }

}