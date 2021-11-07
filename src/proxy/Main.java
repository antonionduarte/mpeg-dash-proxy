package proxy;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import http.HttpClient;
import http.HttpClient10;
import media.MovieManifest;
import media.MovieManifest.Manifest;
import media.MovieManifest.SegmentContent;
import proxy.server.ProxyServer;

public class Main {
	static final String MEDIA_SERVER_BASE_URL = "http://localhost:9999";

	public static void main(String[] args) throws Exception {

		ProxyServer.start((movie, queue) -> new DashPlaybackHandler(movie, queue));
		
	}
	/**
	 * TODO TODO TODO TODO
	 * 
	 * Class that implements the client-side logic.
	 * 
	 * Feeds the player queue with movie segment data fetched
	 * from the HTTP server.
	 * 
	 * The fetch algorithm should prioritize:
	 * 1) avoid stalling the browser player by allowing the queue to go empty
	 * 2) if network conditions allow, retrieve segments from higher quality tracks
	 */
	static class DashPlaybackHandler implements Runnable  {
		final String movie;
		final Manifest manifest;
		final BlockingQueue<SegmentContent> queue;

		final HttpClient http;
		
		DashPlaybackHandler(String movie, BlockingQueue<SegmentContent> queue) {
			this.movie = movie;
			this.queue = queue;
			
			this.http = new HttpClient10();
			String request = MEDIA_SERVER_BASE_URL + "/" + movie + "/manifest.txt";
			String manifestStr = new String(http.doGet(request));

			this.manifest = MovieManifest.parse(manifestStr);
		}


		/**
		 * Runs automatically in a dedicated thread...
		 *
		 * Needs to feed the queue with segment data fast enough to
		 * avoid stalling the browser player
		 *
		 * Upon reaching the end of stream, the queue should
		 * be fed with a zero-length data segment
		 */
		public void run() {
			List<MovieManifest.Track> tracks = manifest.tracks();

			MovieManifest.Track test = tracks.get(3);
			String request = MEDIA_SERVER_BASE_URL + "/" + movie + "/" + test.filename();
			float lastBandwidth = 0;
			float trackBandwidth = test.avgBandwidth();
			System.out.println("TEST BAND: " + trackBandwidth);

			for (int i = 0; i < test.segments().size(); i++) {
				MovieManifest.Segment segment = test.segments().get(i);
				int offset = segment.offset();
				int end = offset + segment.length() - 1;

				long startTime = System.nanoTime();
				byte[] content = http.doGetRange(request, offset, end);
				long endTime = System.nanoTime();

				long elapsedTimeSeconds = (endTime - startTime) / 1_000_000_000;
				lastBandwidth = (((float) content.length * 8) / elapsedTimeSeconds);
				System.out.println("Bandwidth: " + lastBandwidth);
				System.out.println("Queue Size: " + queue.size());

				SegmentContent segmentContent = new SegmentContent(test.contentType(), content);
				try {
					queue.put(segmentContent);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
