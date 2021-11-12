package proxy;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import http.HttpClient;
import http.HttpClient10;
import http.HttpClient11;
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

		private SegmentContent getSegmentContent(String request, MovieManifest.Track track, int index) {
			System.out.println("CURR SEGMENT:" + index);
			MovieManifest.Segment segment = track.segments().get(index);
			int offset = segment.offset();
			int end = offset + segment.length() - 1;
			byte[] content = http.doGetRange(request, offset, end);

			return new SegmentContent(track.contentType(), content);
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
			int trackIndex = tracks.size() - 1; // Start out with the highest quality possible

			MovieManifest.Track test = tracks.get(trackIndex); // We get our track with our trackIndex
			double lastBandwidth = 0; // Used to know which track to download from dependant on the amount of time it takes to download 3 seconds of video
			double trackBandwidth = test.avgBandwidth(); // The bandwith advised to use to get this track without stops
			System.out.println("TEST BAND: " + trackBandwidth);

			for (int i = 0; i < test.segments().size(); i++) {
				String request = MEDIA_SERVER_BASE_URL + "/" + movie + "/" + test.filename(); // We make an HTTP request to get our segments from the HTTP server
				trackBandwidth = test.avgBandwidth();
				boolean changeTrack = false;

				while (lastBandwidth > 0 && lastBandwidth > trackBandwidth && trackIndex < tracks.size() - 1)
				{
					trackIndex++;
					trackBandwidth = tracks.get(trackIndex).avgBandwidth();
					changeTrack = true;
				}
				while (lastBandwidth > 0 && lastBandwidth < trackBandwidth && trackIndex > 0 && !changeTrack)
				{
					trackIndex--;
					trackBandwidth = tracks.get(trackIndex).avgBandwidth();
					changeTrack = true;
				}


				System.out.println(changeTrack);
				if (changeTrack && (i > 1)) {
					test = tracks.get(trackIndex);

					SegmentContent firstSegment = getSegmentContent(request, test, 0);

					try {
						queue.put(firstSegment);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				System.out.println("Track: " + trackIndex);

				double startTime = System.nanoTime();
				SegmentContent segmentContent = getSegmentContent(request, test, i);
				//SegmentContent segmentContent = new SegmentContent(test.contentType(), content);
				double endTime = System.nanoTime();


				double elapsedTimeSeconds = (endTime - startTime) / 1_000_000_000;
				lastBandwidth = (((double) segmentContent.data().length * 8) / elapsedTimeSeconds);
				System.out.println("Bandwidth: " + lastBandwidth);
				System.out.println("Track Bandwidth: " + trackBandwidth);
				System.out.println("Queue Size: " + queue.size());

				try {
					queue.put(segmentContent);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
