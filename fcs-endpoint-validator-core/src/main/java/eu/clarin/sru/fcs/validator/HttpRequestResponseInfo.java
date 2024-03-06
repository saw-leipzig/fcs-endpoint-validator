package eu.clarin.sru.fcs.validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestResponseInfo implements Serializable {
    protected static final Logger logger = LoggerFactory.getLogger(HttpRequestResponseInfo.class);

    private static final long serialVersionUID = 2024_03_04L;

    private HttpRequest request;
    private HttpResponse response;
    private byte[] responseBytes;

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = (request instanceof SerializableHttpRequestWrapper) ? request
                : SerializableHttpRequestWrapper.from(request);
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = (response instanceof SerializableHttpResponseWrapper) ? response
                : SerializableHttpResponseWrapper.from(response);
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public void setResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
    }

    // ----------------------------------------------------------------------

    /*
     * ====================================================================
     * Licensed to the Apache Software Foundation (ASF) under one
     * or more contributor license agreements. See the NOTICE file
     * distributed with this work for additional information
     * regarding copyright ownership. The ASF licenses this file
     * to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing,
     * software distributed under the License is distributed on an
     * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     * KIND, either express or implied. See the License for the
     * specific language governing permissions and limitations
     * under the License.
     * ====================================================================
     *
     * This software consists of voluntary contributions made by many
     * individuals on behalf of the Apache Software Foundation. For more
     * information on the Apache Software Foundation, please see
     * <http://www.apache.org/>.
     *
     */

    @SuppressWarnings("deprecation")
    public static class SerializableAbstractHttpMessage implements HttpMessage, Serializable {
        // see: org.apache.http.message.AbstractHttpMessage

        private static final long serialVersionUID = 2024_03_04L;

        protected ProtocolVersion protocolVersion;
        protected HeaderGroup headergroup = new HeaderGroup();
        protected HttpParams params;

        public static SerializableAbstractHttpMessage from(HttpMessage msg) {
            SerializableAbstractHttpMessage wrapper = new SerializableAbstractHttpMessage();
            wrapper.setParams(msg.getParams());
            wrapper.setHeaders(msg.getAllHeaders());
            wrapper.protocolVersion = msg.getProtocolVersion();
            return wrapper;
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return protocolVersion;
        }

        @Override
        public boolean containsHeader(String name) {
            return this.headergroup.containsHeader(name);
        }

        @Override
        public Header[] getHeaders(String name) {
            return this.headergroup.getHeaders(name);
        }

        @Override
        public Header getFirstHeader(String name) {
            return this.headergroup.getFirstHeader(name);
        }

        @Override
        public Header getLastHeader(String name) {
            return this.headergroup.getLastHeader(name);
        }

        @Override
        public Header[] getAllHeaders() {
            return this.headergroup.getAllHeaders();
        }

        @Override
        public void addHeader(Header header) {
            this.headergroup.addHeader(header);
        }

        @Override
        public void addHeader(String name, String value) {
            Args.notNull(name, "Header name");
            this.headergroup.addHeader(new BasicHeader(name, value));
        }

        @Override
        public void setHeader(Header header) {
            this.headergroup.updateHeader(header);
        }

        @Override
        public void setHeader(String name, String value) {
            Args.notNull(name, "Header name");
            this.headergroup.updateHeader(new BasicHeader(name, value));
        }

        @Override
        public void setHeaders(Header[] headers) {
            this.headergroup.setHeaders(headers);
        }

        @Override
        public void removeHeader(Header header) {
            this.headergroup.removeHeader(header);
        }

        @Override
        public void removeHeaders(String name) {
            if (name == null) {
                return;
            }
            for (final HeaderIterator i = this.headergroup.iterator(); i.hasNext();) {
                final Header header = i.nextHeader();
                if (name.equalsIgnoreCase(header.getName())) {
                    i.remove();
                }
            }
        }

        @Override
        public HeaderIterator headerIterator() {
            return this.headergroup.iterator();
        }

        @Override
        public HeaderIterator headerIterator(String name) {
            return this.headergroup.iterator(name);
        }

        @Override
        public HttpParams getParams() {
            if (this.params == null) {
                this.params = new BasicHttpParams();
            }
            return this.params;
        }

        @Override
        public void setParams(HttpParams params) {
            this.params = Args.notNull(params, "HTTP parameters");
        }

    }

    public static class SerializableHttpRequestWrapper extends SerializableAbstractHttpMessage implements HttpRequest {
        // see: org.apache.http.client.methods.HttpRequestWrapper

        private static final long serialVersionUID = 2024_03_04L;

        protected RequestLine requestLine;

        @SuppressWarnings("deprecation")
        public static SerializableHttpRequestWrapper from(HttpRequest request) {
            SerializableHttpRequestWrapper wrapper = new SerializableHttpRequestWrapper();
            wrapper.setParams(request.getParams());
            wrapper.setHeaders(request.getAllHeaders());
            wrapper.protocolVersion = Optional.ofNullable(request.getRequestLine().getProtocolVersion())
                    .orElse(request.getProtocolVersion());
            // NOTE: need to unwrap to get original URI
            HttpRequest unwrapped = unwrap(request);
            String requestUri = (unwrapped instanceof HttpUriRequest)
                    ? ((HttpUriRequest) unwrapped).getURI().toASCIIString()
                    : unwrapped.getRequestLine().getUri();
            wrapper.requestLine = Optional.ofNullable(unwrapped.getRequestLine())
                    .orElse(new BasicRequestLine(unwrapped.getRequestLine().getMethod(),
                            (requestUri == null || requestUri.isEmpty()) ? "/" : requestUri,
                            wrapper.protocolVersion));
            return wrapper;
        }

        protected static HttpRequest unwrap(HttpRequest request) {
            while (request instanceof HttpRequestWrapper) {
                request = ((HttpRequestWrapper) request).getOriginal();
            }
            return request;
        }

        @Override
        public RequestLine getRequestLine() {
            return requestLine;
        }

    }

    public static class SerializableHttpResponseWrapper extends SerializableAbstractHttpMessage
            implements HttpResponse {
        // see: org.apache.http.impl.execchain.HttpResponseProxy
        // see: org.apache.http.message.BasicHttpResponse

        private static final long serialVersionUID = 2024_03_04L;

        protected StatusLine statusline;
        protected int code;
        protected String reasonPhrase;
        protected HttpEntity entity;
        protected final ReasonPhraseCatalog reasonCatalog = null;
        protected Locale locale = null;

        @SuppressWarnings("deprecation")
        public static SerializableHttpResponseWrapper from(HttpResponse response) {
            SerializableHttpResponseWrapper wrapper = new SerializableHttpResponseWrapper();
            wrapper.setParams(response.getParams());
            wrapper.setHeaders(response.getAllHeaders());

            wrapper.statusline = Args.notNull(response.getStatusLine(), "Status line");
            wrapper.protocolVersion = Optional.ofNullable(response.getStatusLine().getProtocolVersion())
                    .orElse(response.getProtocolVersion());
            wrapper.code = response.getStatusLine().getStatusCode();
            wrapper.reasonPhrase = response.getStatusLine().getReasonPhrase();

            wrapper.entity = response.getEntity();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // make a copy so we can read from it but others can still use it afterwards
                HttpEntity copy = copy(entity);
                response.setEntity(copy);
                wrapper.entity = SerializableByteArrayEntity.from(copy);
            }

            return wrapper;
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

        /**
         * Looks up a reason phrase.
         * This method evaluates the currently set catalog and locale.
         * It also handles a missing catalog.
         *
         * @param code the status code for which to look up the reason
         *
         * @return the reason phrase, or {@code null} if there is none
         */
        protected String getReason(final int code) {
            return this.reasonCatalog != null ? this.reasonCatalog.getReason(code,
                    this.locale != null ? this.locale : Locale.getDefault()) : null;
        }

        @Override
        public StatusLine getStatusLine() {
            if (this.statusline == null) {
                this.statusline = new BasicStatusLine(
                        this.protocolVersion != null ? this.protocolVersion : HttpVersion.HTTP_1_1,
                        this.code,
                        this.reasonPhrase != null ? this.reasonPhrase : getReason(this.code));
            }
            return this.statusline;
        }

        @Override
        public void setStatusLine(StatusLine statusline) {
            throw new UnsupportedOperationException("Readonly!");
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code) {
            throw new UnsupportedOperationException("Readonly!");
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code, String reason) {
            throw new UnsupportedOperationException("Readonly!");
        }

        @Override
        public void setStatusCode(int code) throws IllegalStateException {
            throw new UnsupportedOperationException("Readonly!");
        }

        @Override
        public void setReasonPhrase(String reason) throws IllegalStateException {
            throw new UnsupportedOperationException("Readonly!");
        }

        @Override
        public HttpEntity getEntity() {
            return this.entity;
        }

        @Override
        public void setEntity(HttpEntity entity) {
            throw new UnsupportedOperationException("Readonly!");
        }

        @Override
        public Locale getLocale() {
            return this.locale;
        }

        @Override
        public void setLocale(Locale loc) {
            throw new UnsupportedOperationException("Readonly!");
        }

    }

    public static class SerializableByteArrayEntity extends AbstractHttpEntity implements Serializable {
        // see: org.apache.http.entity.ByteArrayEntity
        // see: org.apache.http.client.entity.EntityBuilder

        private static final long serialVersionUID = 2024_03_04L;

        protected byte[] b = null;
        protected int off = 0;
        protected int len = 0;

        public static SerializableByteArrayEntity from(HttpEntity entity) {
            if (entity == null) {
                return null;
            }

            SerializableByteArrayEntity wrapper = new SerializableByteArrayEntity();

            if (entity.getContentEncoding() != null) {
                wrapper.setContentEncoding(entity.getContentEncoding());
            }

            if (entity.getContentType() != null) {
                wrapper.setContentType(entity.getContentType());
            } else {
                wrapper.setContentType(ContentType.DEFAULT_BINARY.toString());
            }

            wrapper.setChunked(entity.isChunked());

            try (InputStream is = entity.getContent()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is.transferTo(baos);
                wrapper.b = baos.toByteArray();
                wrapper.len = wrapper.b.length;
            } catch (UnsupportedOperationException | IOException e) {
                logger.debug("Error trying to copy HTTP entity input stream", e);
            }

            return wrapper;
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public long getContentLength() {
            return this.len;
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return new ByteArrayInputStream(this.b, this.off, this.len);
        }

        @Override
        public void writeTo(OutputStream outStream) throws IOException {
            Args.notNull(outStream, "Output stream");
            outStream.write(this.b, this.off, this.len);
            outStream.flush();
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

    }

}