package zouxe.streamclient;

import Ice.InitializationData;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
	private StreamPlayer sp = null;
	private AudioRecorder recorder = null;
	private Ice.Communicator communicator = null;
	private MenuItem reconnectButton = null;
	private String artist = null;
	private String title = null;

	private void initIce() {
		try {
			InitializationData initData = new InitializationData();
			initData.properties = Ice.Util.createProperties();
			String address = "zouxe.ovh";
			initData.properties.setProperty("Ice.Default.Router", "Glacier2/router:tcp -h " + address + " -p 4063");
			initData.properties.setProperty("Ice.ACM.Client", "0");
			initData.properties.setProperty("Ice.RetryIntervals", "-1");
			initData.properties.setProperty("CallbackAdapter.Router", "Glacier2/router:tcp -h " + address + " -p 4063");
			communicator = Ice.Util.initialize(initData);
		} catch (Exception e) {
			Log.e("Ice", e.toString());
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initIce();
		connect();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (null != sp)
			sp.destroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		if (null == reconnectButton) {
			reconnectButton = menu.findItem(R.id.action_reconnect);
			if (null != sp)
				sp.setReconnectButton(reconnectButton);
		}
		if (null != sp && sp.isNotWorking())
			reconnectButton.setEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = getLayoutInflater();
			final View v = inflater.inflate(R.layout.settings_dialog, null);
			TextView tv = (TextView) v.findViewById(R.id.server);
			tv.setHint(getText(R.string.address) + " (" + sp.getAddress() + ")");
			tv = (TextView) v.findViewById(R.id.port);
			tv.setHint(getText(R.string.port) + " (" + sp.getPort() + ")");
			builder.setView(v)
					.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							if (sp != null) {
								TextView tv = (TextView) v.findViewById(R.id.server);
								String str = tv.getText().toString();
								sp.setAddress(str);
								tv = (TextView) v.findViewById(R.id.port);
								str = tv.getText().toString();
								sp.setPort(str);
							}
						}
					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
						}
					})
					.show();
			return true;
		} else if (id == R.id.action_reconnect) {
			connect();
		}
		return super.onOptionsItemSelected(item);
	}

	private void connect() {
		if (sp == null || sp.isNotWorking())
			new StreamPlayerLoader().run(communicator, this);
		recorder = new AudioRecorder(this);
	}

	public void search(View searchView) {

		EditText artistText = (EditText) findViewById(R.id.artistSearchText);
		EditText titleText = (EditText) findViewById(R.id.titleSearchText);

		String artist = artistText.getText().toString();
		String title = titleText.getText().toString();

		sp.Search(artist, title);

		titleText.setText("");
		artistText.setText("");
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == 0) {
				String songPath = data.getData().getPath();
				FileUploader uploader = new FileUploader(songPath, artist, title, new CustomProgressDialog(this), sp);
				uploader.execute();
				EditText titleText = (EditText) findViewById(R.id.titleAddText);
				EditText artistText = (EditText) findViewById(R.id.artistAddText);
				titleText.setText("");
				artistText.setText("");
			}
		}
	}

	public void add(View addView) {
		EditText titleText = (EditText) findViewById(R.id.titleAddText);
		EditText artistText = (EditText) findViewById(R.id.artistAddText);
		title = titleText.getText().toString();
		artist = artistText.getText().toString();
		if (title.isEmpty())
			titleText.setHintTextColor(Color.RED);
		else
			titleText.setHintTextColor(Color.LTGRAY);
		if (artist.isEmpty())
			artistText.setHintTextColor(Color.RED);
		else
			artistText.setHintTextColor(Color.LTGRAY);
		if (title.isEmpty() || artist.isEmpty())
			return;
		Intent intent = new Intent(Intent.ACTION_PICK);
		startActivityForResult(intent, 0);
	}

	public void play(View controlView) {
		Button controlButton = (Button) findViewById(R.id.controlButton);
		if (controlButton.getText().equals(getString(R.string.pause)))
			sp.Pause();
		else if (controlButton.getText().equals(getString(R.string.start)))
			sp.Start();
		else if (controlButton.getText().equals(getString(R.string.play)))
			sp.Play();
	}

	public void record(View recordView) {
		Button recordButton = (Button) findViewById(R.id.recordButton);
		if (recordButton.getText().equals(getString(R.string.record))) {
			if (null != recorder) {
				recorder.record();
			}
		} else if (recordButton.getText().equals(getString(R.string.stop))) {
			if (null != recorder) {
				recorder.stopRecord();
			}
		}
	}

	public void audioPlaySong(String artist, String title) {
		sp.getSong(artist, title);
		sp.Start();
	}

	public void audioAddSong(String artist, String title) {
		this.artist = artist;
		this.title = title;
		Intent intent = new Intent(Intent.ACTION_PICK);
		startActivityForResult(intent, 0);
	}

	public void audioRemoveSong(String artist, String title) {
		sp.getSong(artist, title);
		sp.removeSong();
	}

	public void audioSearchSong(String artist, String title) {
		sp.Search(artist, title);
	}

	public void remove(View removeView) {
		sp.removeSong();
	}

	private class StreamPlayerLoader extends Thread {
		public void run(Ice.Communicator communicator, Activity act) {
			sp = new StreamPlayer(communicator, act);
		}
	}
}
