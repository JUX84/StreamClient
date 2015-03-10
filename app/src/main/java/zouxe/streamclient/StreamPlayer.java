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
			if (!str.equals(lastAction))
				Log.v("Notification", str);
			lastAction = "";
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
	private Ice.Communicator communicator = null;
	private Glacier2.RouterPrx router = null;
	private boolean connected = false;
	private String lastAction = "";

	public StreamPlayer(Activity activity) {
		this.activity = activity;

		controlButton = (Button) activity.findViewById(R.id.controlButton);
		removeButton = (Button) activity.findViewById(R.id.removeButton);
		try {
			setStatus(activity.getString(R.string.connecting));
			initIce();
			initRouter();
			initServer();
			if (isWorking) {
				mp = new MediaPlayer();
				setStatus(activity.getString(R.string.connected));
				setControlsEnabled(true);
			} else {
				setStatus(activity.getString(R.string.disconnected));
			}
			initIceStorm();
		} catch (Ice.LocalException e) {
			new AlertDialog.Builder(activity).setMessage(activity.getText(R.string.connectionTo) + " " + address + " " + activity.getText(R.string.fail) + ".\n" + activity.getText(R.string.disconnectedReasonServer)).create().show();
			setStatus(activity.getString(R.string.disconnected));
			Log.e("StreamPlayer", e.toString());
		}
	}

	private void initIce() {
		try {
			InitializationData initData = new InitializationData();
			initData.properties = Ice.Util.createProperties();
			initData.properties.setProperty("Ice.Default.Router", "Glacier2/router:tcp -h "+address+" -p 4063");
			initData.properties.setProperty("Ice.ACM.Client", "0");
			initData.properties.setProperty("Ice.RetryIntervals" ,"-1");
			initData.properties.setProperty("CallbackAdapter.Router", "Glacier2/router:tcp -h "+address+" -p 4063");
			communicator = Ice.Util.initialize(initData);
		} catch (Exception e) {
			Log.e("Ice", e.toString());
		}
	}

	private void initRouter() {
		if (communicator == null)
			return;
		try {
			Ice.RouterPrx defaultRouter = communicator.getDefaultRouter();
			router = Glacier2.RouterPrxHelper.checkedCast(defaultRouter);
			try {
				router.createSession("zouxe", "zouxe");
				connected = true;
			} catch(Exception e) {
				Log.e("Glacier2Login", e.toString());
			}
		} catch (Exception e) {
			Log.e("Glacier2", e.toString());
		}
	}

	private void initServer() {
		if (communicator == null || !connected)
			return;
		try {
			Ice.ObjectPrx base = communicator.stringToProxy("StreamMetaServer:tcp -h " + address + " -p " + port);
			server = Player.ServerPrxHelper.checkedCast(base);
			isWorking = true;
		} catch (Exception e) {
			Log.e("StreamMetaServer", e.toString());
		}
	}

	private void initIceStorm() {
		if (communicator == null || !connected)
			return;
		try {
			Ice.ObjectPrx obj = communicator.stringToProxy("IceStorm/TopicManager:tcp -h " + address + " -p 9999");
			IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj);
			Ice.ObjectAdapter adapter = communicator.createObjectAdapterWithRouter("MonitorAdapter", router);
			Monitor monitor = new MonitorI();
			Ice.ObjectPrx proxy = adapter.add(monitor, new Identity("default", router.getCategoryForClient())).ice_twoway();
			adapter.activate();
			IceStorm.TopicPrx topic;
			try {
				topic = topicManager.retrieve("StreamPlayerNotifs");
				try {
					topic.subscribeAndGetPublisher(null, proxy);
				} catch(Exception e) {
					Log.e("IceStormSubscribe", e.toString());
				}
			} catch (Exception e) {
				Log.e("IceStormTopic", e.toString());
			}
		} catch (Exception e) {
			Log.e("IceStorm", e.toString());
		}
	}

	private void setControlsEnabled(boolean b) {
		EditText artistAdd = (EditText) activity.findViewById(R.id.artistAddText);
		EditText titleAdd = (EditText) activity.findViewById(R.id.titleAddText);
		EditText artistSearch = (EditText) activity.findViewById(R.id.artistSearchText);
		EditText titleSearch = (EditText) activity.findViewById(R.id.titleSearchText);
		Button addButton = (Button) activity.findViewById(R.id.addButton);
		Button searchButton = (Button) activity.findViewById(R.id.searchButton);
		artistAdd.setEnabled(b);
		titleAdd.setEnabled(b);
		artistSearch.setEnabled(b);
		titleSearch.setEnabled(b);
		addButton.setEnabled(b);
		searchButton.setEnabled(b);
	}

	public boolean isNotWorking() {
		if (server == null) {
			isWorking = false;
		} else {
			try {
				server.ice_ping();
			} catch (Ice.LocalException e) {
				server = null;
				isWorking = false;
				connected = false;
				setControlsEnabled(false);
				new AlertDialog.Builder(activity).setMessage(activity.getText(R.string.connectionTo) + " " + address + " " + activity.getText(R.string.fail) + ".\n" + activity.getText(R.string.disconnectedReasonServer)).create().show();
				setStatus(activity.getString(R.string.disconnected));
			}
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
		lastAction = "The song "+selectedSong.title+" by "+selectedSong.artist+" was removed";
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
		lastAction = "The song "+title+" by "+artist+" was added";
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
