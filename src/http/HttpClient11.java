package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

/*
* TODO: Add the HOST header to the Requests.
 */
public class HttpClient11 implements HttpClient {

    private static final String HTTP_SUCCESS = "20";
    private static final String GET_FORMAT_STR = "GET %s HTTP/1.1";

    private static Socket socket = null;

    static private byte[] getContents(InputStream in) throws IOException {
        String reply = Http.readLine(in);
        System.out.println(reply);
        if (!reply.contains(HTTP_SUCCESS)) {
            throw new RuntimeException(String.format("HTTP request failed: [%s]", reply));
        }

        while ((reply = Http.readLine(in)).length() > 0) {
            // System.out.println(reply);
        }

        byte[] bytes = in.readAllBytes();
        in.close();

        return bytes;
    }

    @Override
    public byte[] doGet(String url) {
        try {
            URL u = new URL(url);

            if (socket == null) {
                int port = u.getPort();
                socket = new Socket(u.getHost(), port > 0 ? port : HTTP_DEFAULT_PORT);
            }

            String request = String.format(GET_FORMAT_STR, u.getFile()) + "\r\n"
                    + USER_AGENT + "\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());
            os.close();

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

            if (socket == null) {
                int port = u.getPort();
                socket = new Socket(u.getHost(), port > 0 ? port : HTTP_DEFAULT_PORT);
            }

            String request = String.format(GET_FORMAT_STR, u.getPath()) + "\r\n"
                    + String.format("Range: bytes=%s-\r\n", start)
                    + USER_AGENT + "\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());
            os.close();

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

            if (socket == null) {
                int port = u.getPort();
                socket = new Socket(u.getHost(), port > 0 ? port : HTTP_DEFAULT_PORT);
            }

            String request = String.format(GET_FORMAT_STR, u.getPath()) + "\r\n"
                    + String.format("Range: bytes=%s-%s\r\n", start, end)
                    + USER_AGENT + "\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());
            os.close();

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
}
