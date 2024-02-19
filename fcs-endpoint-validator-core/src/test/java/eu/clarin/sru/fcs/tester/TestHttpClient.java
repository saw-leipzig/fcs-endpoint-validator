package eu.clarin.sru.fcs.validator;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

// from : https://stackoverflow.com/a/25290882/9360161

public class TestHttpClient {

    public static void main(String[] args) {
        String s = new TestHttpClient().get("http://onet.pl");
        System.out.println("---------------------\n" +
                "RESULT:\n" +
                "---------------------" +
                "\n" + s.substring(0, Math.min(s.length(), 200)));
    }

    public String get(String uri) {
        StreamRecorder streamRecorder = new StreamRecorder();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new ListeningSocketFactory(streamRecorder))
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        try (CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connManager).build();
                CloseableHttpResponse response1 = httpclient.execute(new HttpGet(uri))) {
            HttpEntity entity = response1.getEntity();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            entity.writeTo(byteArrayOutputStream);
            // System.out.println(byteArrayOutputStream.toString());
            // return new String(streamRecorder.getRecordedStreamAsCharArray());
            return streamRecorder.getRecordedStreamAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ListeningSocketFactory extends PlainConnectionSocketFactory {
        private final CopyInputStream.InputStreamListener streamListener;

        public ListeningSocketFactory(StreamRecorder streamListener) {
            this.streamListener = streamListener;
        }

        @Override
        public Socket createSocket(HttpContext context) throws IOException {
            // return new SocketWithInputStreamListener(streamListener);
            return new Socket() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return new CopyInputStream(super.getInputStream(), streamListener);
                }
            };
        }

    }

    public static class StreamRecorder implements CopyInputStream.InputStreamListener {
        // private final List<Integer> streamCopy = new ArrayList<>(1000);
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public synchronized void processByte(int readByte) {
            // streamCopy.add(readByte);
            baos.write(readByte);
        }

        // public synchronized char[] getRecordedStreamAsCharArray() {
        // char[] result = new char[streamCopy.size()];
        // for (int i = 0; i < streamCopy.size(); i++) {
        // // result[i] = (char) Character.toChars(streamCopy.get(i))[0];
        // result[i] = (char) streamCopy.get(i).intValue();
        // }
        // return result;
        // }

        public synchronized String getRecordedStreamAsString() {
            return baos.toString();
        }
    }

    public static class CopyInputStream extends FilterInputStream {
        private final InputStreamListener streamListener;

        CopyInputStream(InputStream in, InputStreamListener streamListener) {
            super(in);
            this.streamListener = streamListener;
        }

        @Override
        public int read() throws IOException {
            int readByte = super.read();
            processByte(readByte);
            return readByte;
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            int readBytes = super.read(buffer, offset, count);
            processBytes(buffer, offset, readBytes);
            return readBytes;
        }

        private void processBytes(byte[] buffer, int offset, int readBytes) {
            for (int i = 0; i < readBytes; i++) {
                processByte(buffer[i + offset]);
            }
        }

        private void processByte(int readByte) {
            streamListener.processByte(readByte);
        }

        interface InputStreamListener {
            void processByte(int readByte);
        }
    }
}
