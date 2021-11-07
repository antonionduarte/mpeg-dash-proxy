package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

/**
 * Implements a basic HTTP1.0 client
 * @author smduarte
 *
 */
public class HttpClient10 implements HttpClient {

	private static final String HTTP_SUCCESS = "20";
	private static final String GET_FORMAT_STR = "GET %s HTTP/1.0";

	static private byte[] getContents(InputStream in) throws IOException {
		String reply = Http.readLine(in);
		System.out.println(reply);
		if (!reply.contains(HTTP_SUCCESS)) {
			throw new RuntimeException(String.format("HTTP request failed: [%s]", reply));
		}

		while ((reply = Http.readLine(in)).length() > 0) {
			//System.out.println(reply);
		}

		return in.readAllBytes();
	}
	
	@Override
	public byte[] doGet(String urlStr) {
		try {
			URL url = new URL(urlStr);
			int port = url.getPort();
			try (Socket cs = new Socket(url.getHost(), port < 0 ? url.getDefaultPort(): port)) {
				String request = String.format(GET_FORMAT_STR, url.getFile()) + "\r\n"
								+ USER_AGENT + "\r\n\r\n";

				cs.getOutputStream().write(request.getBytes());
				return getContents(cs.getInputStream());
			}
		} catch (Exception x) {
			x.printStackTrace();
			return null;
		}
	}

	@Override
	public byte[] doGetRange(String url, long start) {
		try {
			URL u = new URL(url);
			int port = u.getPort();
			try (Socket cs = new Socket(u.getHost(), port > 0 ? port : HTTP_DEFAULT_PORT)) {
				String request = String.format(GET_FORMAT_STR, u.getPath()) + "\r\n"
								+ String.format("Range: bytes=%s-\r\n", start)
								+ USER_AGENT + "\r\n\r\n";

				cs.getOutputStream().write(request.getBytes());
				return getContents(cs.getInputStream());
			}
		} catch (Exception x) {
			x.printStackTrace();
			return null;
		}
	}

	@Override
	public byte[] doGetRange(String url, long start, long end) {
		try {
			URL u = new URL(url);
			int port = u.getPort();
			try (Socket cs = new Socket(u.getHost(), port > 0 ? port : HTTP_DEFAULT_PORT)) {
				String request = String.format(GET_FORMAT_STR, u.getPath()) + "\r\n"
								+ String.format("Range: bytes=%s-%s\r\n", start, end)
								+ USER_AGENT + "\r\n\r\n";

				cs.getOutputStream().write(request.getBytes());
				return getContents(cs.getInputStream());
			}
		} catch (Exception x) {
			x.printStackTrace();
			return null;
		}
	}
}
