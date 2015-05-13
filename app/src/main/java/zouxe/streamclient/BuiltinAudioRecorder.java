package zouxe.streamclient;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.ImageButton;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.List;

class BuiltinAudioRecorder implements AudioRecorder {
	private final MainActivity activity;
	private final ImageButton recordButton;
	private SpeechRecognizer asr = null;
	private Intent intent = null;
	private boolean recording = false;

	public BuiltinAudioRecorder(MainActivity activity) {
		this.activity = activity;
		recordButton = (ImageButton) activity.findViewById(R.id.recordButton);
		asr = SpeechRecognizer.createSpeechRecognizer(activity);
		asr.setRecognitionListener(new ResultProcessor());
		intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
	}

	public void record() {
		recording = true;
		recordButton.setImageResource(R.drawable.ic_mic_white_24dp);
		asr.startListening(intent);
	}

	public void stopRecord() {
		recording = false;
		recordButton.setImageResource(R.drawable.ic_mic_none_white_24dp);
		asr.stopListening();
	}

	@Override
	public boolean isRecording() {
		return recording;
	}

	private class ResultProcessor implements RecognitionListener {

		@Override
		public void onReadyForSpeech(Bundle params) {
		}

		@Override
		public void onBeginningOfSpeech() {
		}

		@Override
		public void onRmsChanged(float rmsdB) {
		}

		@Override
		public void onBufferReceived(byte[] buffer) {
		}

		@Override
		public void onEndOfSpeech() {
		}

		@Override
		public void onError(int error) {
			String message;
			switch (error) {
				case SpeechRecognizer.ERROR_AUDIO:
					message = "Audio recording error.";
					break;
				case SpeechRecognizer.ERROR_CLIENT:
					message = "Other client side errors.";
					break;
				case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
					message = "Insufficient permissions";
					break;
				case SpeechRecognizer.ERROR_NETWORK:
					message = "Other network related errors.";
					break;
				case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
					message = "Network operation timed out.";
					break;
				case SpeechRecognizer.ERROR_NO_MATCH:
					message = "No recognition result matched.";
					break;
				case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
					message = "RecognitionService busy.";
					break;
				case SpeechRecognizer.ERROR_SERVER:
					message = "Server sends error status.";
					break;
				case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
					message = "No speech input";
					break;
				default:
					message = "Error";
			}
			Log.e("SpeechRecognizer", message);
		}

		@Override
		public void onResults(Bundle results) {
			List<String> tmp = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			Log.v("BuiltinText", tmp.get(0));
			try {
				RequestQueue queue = Volley.newRequestQueue(activity);
				String url = "http://zouxe.ovh:8080/CommandParser/webresources/api?lang=en&str=" + URLEncoder.encode(tmp.get(0), "UTF-8");
				StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						try {
							JSONObject object = new JSONObject(response);
							JSONObject song = new JSONObject(object.getString("song"));
							String command = object.getString("command");
							String artist = song.getString("artist");
							String title = song.getString("title");
							Log.v("CommandParserSuccess", command + " " + artist + " " + title);
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
			stopRecord();
		}

		@Override
		public void onPartialResults(Bundle partialResults) {
		}

		@Override
		public void onEvent(int eventType, Bundle params) {
		}
	}
}
