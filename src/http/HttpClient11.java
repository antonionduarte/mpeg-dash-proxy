package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

public class HttpClient11 implements HttpClient {

    private static final String HTTP_SUCCESS = "20";
    private static final String GET_FORMAT_STR = "GET %s HTTP/1.1";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String HOST_HEADER = "Host: localhost:8080";
    private static final String RANGE_HEADER = "Range: bytes=%s-%s";

    private static Socket socket = null;

    static private byte[] getContents(InputStream in) throws IOException {
        String reply = Http.readLine(in);

        if (!reply.contains(HTTP_SUCCESS)) {
            throw new RuntimeException(String.format("HTTP request failed: [%s]", reply));
        }

        int contentLength = 0;
        while ((reply = Http.readLine(in)).length() > 0) {
            System.out.println(reply);
            String[] headerParts = Http.parseHttpHeader(reply);
            if (headerParts != null && headerParts[0].equalsIgnoreCase(CONTENT_LENGTH_HEADER)) {
                contentLength = Integer.parseInt(headerParts[1]);
            }
        }

        return in.readNBytes(contentLength);
    }

    @Override
    public byte[] doGet(String url) {
        try {
            URL u = new URL(url);

            handleSocket(u);

            String request = String.format(GET_FORMAT_STR, u.getFile()) + "\r\n"
                    + HOST_HEADER + "\r\n"
                    + USER_AGENT + "\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());

            return getContents(socket.getInputStream());
        } catch (Exception x) {
            x.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] doGetRange(String url, long start) {
        try {
            URL u = new URL(url);

            handleSocket(u);

            String request = String.format(GET_FORMAT_STR, u.getPath()) + "\r\n"
                    + HOST_HEADER + "\r\n"
                    + String.format(RANGE_HEADER + "\r\n", start, "")
                    + USER_AGENT + "\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());

            return getContents(socket.getInputStream());
        } catch (Exception x) {
            x.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] doGetRange(String url, long start, long end) {
        try {
            URL u = new URL(url);

            handleSocket(u);

            String request = String.format(GET_FORMAT_STR, u.getPath()) + "\r\n"
                    + HOST_HEADER + "\r\n"
                    + String.format(RANGE_HEADER + "\r\n", start, end)
                    + USER_AGENT + "\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());

            return getContents(socket.getInputStream());
        } catch (Exception x) {
            x.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
        socket = null;
    }

    private void handleSocket(URL url) throws IOException {
        if (socket == null || socket.isClosed()) {
            if (socket != null) {
                socket.close();
            }

            socket = new Socket(url.getHost(), url.getPort() > 0 ? url.getPort() : HTTP_DEFAULT_PORT);
        }
    }
}
