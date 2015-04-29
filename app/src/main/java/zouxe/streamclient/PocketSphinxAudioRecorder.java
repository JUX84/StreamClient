package zouxe.streamclient;

import PocketSphinxIce.IPocketSphinxServerPrx;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Created by JUX on 19/04/2015.
 */
class PocketSphinxAudioRecorder {
	private static final int REC_SR = 16000;
	private static final int REC_CHAN = AudioFormat.CHANNEL_IN_MONO;
	private static final int REC_ENC = AudioFormat.ENCODING_PCM_16BIT;
	private static int bufferSize;
	private final MainActivity activity;
	private final Button recordButton;
	private AudioRecord recorder = null;
	private Ice.Communicator communicator = null;
	private IPocketSphinxServerPrx server = null;
	private short[] audioData;
	private int current;

	public PocketSphinxAudioRecorder(Ice.Communicator communicator, MainActivity activity) {
		this.communicator = communicator;
		this.activity = activity;
		recordButton = (Button) activity.findViewById(R.id.recordButton);
		initServer();
	}

	public void record() {
		recordButton.setText(R.string.stop);
		bufferSize = AudioRecord.getMinBufferSize(REC_SR, REC_CHAN, REC_ENC);
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				REC_SR, REC_CHAN,
				REC_ENC, bufferSize);
		recorder.startRecording();
		audioData = new short[bufferSize * 100];
		current = 0;
		new Thread(new Runnable() {
			public void run() {
				int tmp;
				while (current < bufferSize * 100 && recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
					tmp = recorder.read(audioData, current, bufferSize);
					current += tmp;
				}
				stopRecord();
			}
		}).start();
	}

	private void initServer() {
		try {
			Ice.ObjectPrx base = communicator.stringToProxy("PocketSphinxServer:tcp -h server.datdroplet.ovh -p 20000");
			server = PocketSphinxIce.IPocketSphinxServerPrxHelper.checkedCast(base);
		} catch (Exception e) {
			Log.e("PocketSphinxServer", e.toString());
		}
	}

	private void stopRecord() {
		if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			recorder.stop();
			new Thread(new Runnable() {
				public void run() {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							recordButton.setText("...");
							recordButton.setEnabled(false);
						}
					});
					try {
						String text = server.decode(Arrays.copyOf(audioData, current));
						try {
							RequestQueue queue = Volley.newRequestQueue(activity);
							String url = "http://zouxe.ovh:8080/CommandParser/webresources/api?str=" + URLEncoder.encode(text, "UTF-8");
							StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
								@Override
								public void onResponse(String response) {
									try {
										JSONObject object = new JSONObject(response);
										JSONObject song = new JSONObject(object.getString("song"));
										String command = object.getString("command");
										String artist = song.getString("artist");
										String title = song.getString("title");
										switch (command) {
											case "listen":
												activity.audioPlaySong(artist, title);
												break;
											case "add":
												activity.audioAddSong(artist, title);
												break;
											case "remove":
												activity.audioRemoveSong(artist, title);
												break;
											case "search":
												activity.audioSearchSong(artist, title);
												break;
											default:
												throw new Exception("error");
										}
									} catch (Exception e) {
										try {
											JSONObject object = new JSONObject(response);
											String error = object.getString("error");
											Log.e("CommandParser", error);
										} catch (JSONException e2) {
											e2.printStackTrace();
										}
									}
								}
							}, new Response.ErrorListener() {
								@Override
								public void onErrorResponse(VolleyError error) {
									Log.e("CommandParser", error.toString());
								}
							});
							queue.add(stringRequest);
						} catch (Exception e) {
							Log.e("text2command", e.toString());
						}
					} catch (Exception e) {
						Log.e("audio2text", e.toString());
					}
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							recordButton.setEnabled(true);
							recordButton.setText(R.string.record);
						}
					});
				}
			}).start();

		}
	}
}
