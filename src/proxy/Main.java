package proxy;

import java.io.ByteArrayOutputStream;
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
			int trackIndex = 0;

			MovieManifest.Track test = tracks.get(trackIndex); // We get our track with our trackIndex
			double lastBandwidth = 0; // Used to know which track to download from dependant on the amount of time it takes to download 3 seconds of video
			for (int i = 0; i < test.segments().size(); i++) {
				boolean changeTrack = false;


				while (trackIndex < tracks.size() - 1 && lastBandwidth > tracks.get(trackIndex+1).avgBandwidth())
				{
					trackIndex++;
					changeTrack = true;
				}
				while (trackIndex > 0 && lastBandwidth < tracks.get(trackIndex).avgBandwidth())
				{

					trackIndex--;
					changeTrack = true;
				}
				if(queue.size()*test.segmentDuration() <= test.segmentDuration() && trackIndex > 0)
					trackIndex--;

				String request = MEDIA_SERVER_BASE_URL + "/" + movie + "/" + test.filename(); // We make an HTTP request to get our segments from the HTTP servers
				SegmentContent firstSegment = null;
				if (changeTrack) {
					test = tracks.get(trackIndex);
					request = MEDIA_SERVER_BASE_URL + "/" + movie + "/" + test.filename();

					firstSegment = getSegmentContent(request, test, 0);

				}
				double startTime = System.nanoTime();
				SegmentContent segmentContent = getSegmentContent(request, test, i);
				double endTime = System.nanoTime();

				if(firstSegment != null){
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					try {
						out.write(firstSegment.data());
						out.write(segmentContent.data());
					} catch (Exception e){
						e.printStackTrace();
					}
					segmentContent = new SegmentContent(test.contentType(), out.toByteArray());
				}
				double elapsedTimeSeconds = (endTime - startTime) / 1_000_000_000;
				lastBandwidth = (((double) segmentContent.data().length * 8) / (elapsedTimeSeconds));

				try {
					queue.put(segmentContent);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			try {
				queue.put(new SegmentContent(test.contentType(), new byte[0]));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
