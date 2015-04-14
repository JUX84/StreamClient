package zouxe.streamclient;

import PocketSphinxIce.IPocketSphinxServerPrx;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
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
 * Created by JUX on 14/04/2015.
 */
public class AudioProcessor {
	private short[] data;
	private boolean valid = false;
	private String command = null;
	private String artist = null;
	private String title = null;
	private String text = null;
	private IPocketSphinxServerPrx server = null;
	private Activity activity = null;

	public AudioProcessor(short[] data, IPocketSphinxServerPrx server, Activity activity) {
		this.data = data;
		this.server = server;
		this.activity = activity;
	}

	public void audio2text() {
		try {
			text = server.decode(data);
		} catch (Exception e) {
			Log.e("audio2text", e.toString());
		}
	}

	public void text2command() {
		try {
			RequestQueue queue = Volley.newRequestQueue(activity);
			String url = "http://zouxe.ovh:8080/CommandParser/webresources/api?str=" + URLEncoder.encode(text, "UTF-8");
			StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
				@Override
				public void onResponse(String response) {
					try {
						JSONObject object = new JSONObject(response);
						JSONObject song = new JSONObject(object.getString("song"));
						command = object.getString("command");
						artist = song.getString("artist");
						title = song.getString("title");
						valid = true;
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Toast.makeText(activity.getApplicationContext(), "Didn't work", Toast.LENGTH_SHORT).show();
				}
			});
			queue.add(stringRequest);
		} catch (Exception e) {
			Log.e("text2command", e.toString());
		}
	}

	public String getText() {
		return text;
	}

	public boolean isValid() {
		return valid;
	}

	public String getCommand() {
		return command;
	}

	public String getArtist() {
		return artist;
	}

	public String getTitle() {
		return title;
	}
}
