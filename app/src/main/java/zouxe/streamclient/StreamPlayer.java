package zouxe.streamclient;

import Ice.Current;
import Ice.Identity;
import Ice.InitializationData;
import Player.Monitor;
import Player.Song;
import Player._MonitorDisp;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.util.*;

class StreamPlayer implements MediaPlayer.OnPreparedListener {
	private class MonitorI extends _MonitorDisp {
		public void report(String str, Current c) {
			Log.v("Notif", str);
		}
	}

	private MediaPlayer mp = null;
	private Player.ServerPrx server = null;
	private String token = null;
	private Song[] songs = null;
	private Song playingSong = null;
	private Song selectedSong = null;
	private View v = null;
	private Button controlButton = null;
	private Button removeButton = null;
	private boolean isLoading = false;
	private Activity activity = null;
	private String address = "80.240.129.188";
	private String port = "10001";
	private boolean isWorking = false;

	public StreamPlayer(Activity activity) {
		this.activity = activity;

		controlButton = (Button) activity.findViewById(R.id.controlButton);
		removeButton = (Button) activity.findViewById(R.id.removeButton);
		EditText artistAdd = (EditText) activity.findViewById(R.id.artistAddText);
		EditText titleAdd = (EditText) activity.findViewById(R.id.titleAddText);
		EditText artistSearch = (EditText) activity.findViewById(R.id.artistSearchText);
		EditText titleSearch = (EditText) activity.findViewById(R.id.titleSearchText);
		try {
			InitializationData initData = new InitializationData();
			initData.properties = Ice.Util.createProperties();
			initData.properties.setProperty("Ice.Default.Router", "Glacier2/router:tcp -h "+address+" -p 4063");
			initData.properties.setProperty("Ice.ACM.Client", "0");
			initData.properties.setProperty("Ice.ACM.Server", "0");
			initData.properties.setProperty("Ice.RetryIntervals" ,"-1");
			/*initData.properties.setProperty("Ice.Trace.Network", "1");
			initData.properties.setProperty("Ice.Trace.Protocol", "1");
			initData.properties.setProperty("Ice.Warn.Connections", "1");*/
			initData.properties.setProperty("CallbackAdapter.Router", "Glacier2/router:tcp -h "+address+" -p 4063");
			Ice.Communicator ic = Ice.Util.initialize(initData);
			Ice.RouterPrx defaultRouter = ic.getDefaultRouter();
			Glacier2.RouterPrx router = Glacier2.RouterPrxHelper.checkedCast(defaultRouter);
			String username = "zouxe";
			String password = "zouxe";
			Glacier2.SessionPrx session;
			try
			{
				session = router.createSession(username, password);
			}
			catch(Glacier2.PermissionDeniedException ex)
			{
				Log.e("Glacier2", "permission denied: " + ex.reason);
			}
			catch(Glacier2.CannotCreateSessionException ex)
			{
				Log.e("Glacier2", "cannot create session: " + ex.reason);
			}
			setStatus(activity.getString(R.string.connecting));
			Ice.ObjectPrx base = ic.stringToProxy("StreamMetaServer:tcp -h " + address + " -p " + port);
			server = Player.ServerPrxHelper.checkedCast(base);
			if (server == null) {
				new AlertDialog.Builder(activity).setMessage(activity.getText(R.string.connectionTo) + " " + address + " " + activity.getText(R.string.success) + ".\n" + activity.getText(R.string.disconnectedReasonCast)).create().show();
				setStatus(activity.getString(R.string.disconnected));
			}
			setStatus(activity.getString(R.string.connected));
			mp = new MediaPlayer();
			Button b = (Button) activity.findViewById(R.id.addButton);
			b.setEnabled(true);
			b = (Button) activity.findViewById(R.id.searchButton);
			b.setEnabled(true);
			artistAdd.setEnabled(true);
			titleAdd.setEnabled(true);
			artistSearch.setEnabled(true);
			titleSearch.setEnabled(true);
			isWorking = true;
			Ice.ObjectPrx obj = ic.stringToProxy("IceStorm/TopicManager:tcp -h " + address + " -p 9999");
			IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj);
			Ice.ObjectAdapter adapter = ic.createObjectAdapterWithRouter("MonitorAdapter", router);
			Monitor monitor = new MonitorI();
			Ice.ObjectPrx proxy = adapter.add(monitor, new Identity("test", router.getCategoryForClient())).ice_twoway();
			adapter.activate();
			IceStorm.TopicPrx topic;
			try {
				topic = topicManager.retrieve("StreamPlayerNotifs");
				try {
					topic.subscribeAndGetPublisher(null, proxy);
				} catch(Exception e) {
				}
			}
			catch (IceStorm.NoSuchTopic ex) {
				Log.e("Ice", ex.getMessage());
			}
		} catch (Ice.LocalException e) {
			new AlertDialog.Builder(activity).setMessage(activity.getText(R.string.connectionTo) + " " + address + " " + activity.getText(R.string.fail) + ".\n" + activity.getText(R.string.disconnectedReasonServer)).create().show();
			setStatus(activity.getString(R.string.disconnected));
			Log.e("StreamPlayer", e.getMessage());
		}
	}

	public boolean isNotWorking() {
		if (server != null) {
			try {
				server.ice_ping();
			} catch (Ice.LocalException e) {
				isWorking = false;
				new AlertDialog.Builder(activity).setMessage(activity.getText(R.string.connectionTo) + " " + address + " " + activity.getText(R.string.fail) + ".\n" + activity.getText(R.string.disconnectedReasonServer)).create().show();
				setStatus(activity.getString(R.string.disconnected));
			}
		} else {
			isWorking = false;
		}
		return !isWorking;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		if (!address.isEmpty())
			this.address = address;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		if (!port.isEmpty())
			this.port = port;
	}

	private void setStatus(String str) {
		TextView status = (TextView) activity.findViewById(R.id.serverStatus);
		status.setText(activity.getString(R.string.serverStatus) + " " + str);
	}

	private void selectSong(Song s) {
		selectedSong = s;
		if (playingSong != null && playingSong.equals(selectedSong)) {
			if (isLoading) {
				controlButton.setText(activity.getString(R.string.loading));
				controlButton.setEnabled(false);
			} else {
				if (mp.isPlaying())
					controlButton.setText(activity.getString(R.string.pause));
				else
					controlButton.setText(activity.getString(R.string.play));
				controlButton.setEnabled(true);
			}
		} else {
			controlButton.setText(activity.getString(R.string.start));
			controlButton.setEnabled(true);
			removeButton.setText(activity.getString(R.string.remove));
		}
		removeButton.setEnabled(true);
	}

	public void removeSong() {
		if (selectedSong == null || server == null || isNotWorking())
			return;
		if (playingSong == selectedSong)
			Pause();
		server.removeSong(selectedSong);
		selectedSong = null;
		controlButton.setEnabled(false);
		removeButton.setEnabled(false);
	}

	public void Start() {
		if (selectedSong == null || server == null || isNotWorking())
			return;
		if (token != null) {
			mp.stop();
			server.stopSong(token);
		}
		playingSong = selectedSong;
		token = server.selectSong(selectedSong);
		server.playSong(token);
		String mp3 = "http://zouxe.ovh:8090/" + token + ".mp3";
		mp.reset();
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mp.setOnPreparedListener(this);
		try {
			mp.setDataSource(mp3);
		} catch (IOException e) {
			Log.w("StreamPlayer", e.getMessage());
			mp.reset();
			isLoading = false;
			return;
		}
		controlButton.setText(activity.getString(R.string.loading));
		controlButton.setEnabled(false);
		isLoading = true;
		mp.prepareAsync();
	}

	public void Pause() {
		if (server == null || token == null || isNotWorking())
			return;
		controlButton.setText(activity.getString(R.string.play));
		controlButton.setEnabled(true);
		mp.pause();
	}

	public void Play() {
		if (server == null || token == null || isNotWorking())
			return;
		if (selectedSong.equals(playingSong)) {
			controlButton.setText(activity.getString(R.string.pause));
			controlButton.setEnabled(true);
		}
		isLoading = false;
		mp.start();
	}

	public void Search(String artist, String title) {
		if (server == null || isNotWorking())
			return;
		new SearchSongLoader().run(artist, title);
	}

	public void addSong(String artist, String title) {
		if (server == null || isNotWorking())
			return;
		server.addSong(new Song(artist, title, artist + "." + title + ".mp3"));
	}

	public void onPrepared(MediaPlayer mp) {
		Play();
	}

	private class SearchSongLoader extends Thread {
		public void run(String artist, String title) {
			songs = server.searchSong(artist, title);

			ListView lv = (ListView) activity.findViewById(R.id.listView);

			List<Map<String, String>> array = new ArrayList<>();
			for (Song s : songs)
				array.add(putData(s.artist, s.title));

			String[] from = {"title", "artist"};
			int[] to = {android.R.id.text1, android.R.id.text2};

			SimpleAdapter adapter = new SimpleAdapter(activity, array, android.R.layout.simple_list_item_2, from, to);
			lv.setAdapter(adapter);

			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, final View view,
				                        int position, long id) {
					if (v != null)
						v.setBackgroundColor(Color.TRANSPARENT);
					view.setBackgroundColor(Color.LTGRAY);
					v = view;
					selectSong(songs[position]);
				}
			});
		}

		private HashMap<String, String> putData(String artist, String title) {
			HashMap<String, String> item = new HashMap<>();
			item.put("artist", artist);
			item.put("title", title);
			return item;
		}
	}
}
