package zouxe.streamclient;

import Player.Song;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * Created by JUX on 14/04/2015.
 */
class FileUploader extends AsyncTask<Void, Integer, Void> {
	private String songPath = null;
	private String artist = null;
	private String title = null;
	private CustomProgressDialog dialog = null;
	private Player.ServerPrx server = null;
	private int size = 0;

	public FileUploader(String songPath, String artist, String title, CustomProgressDialog dialog, Player.ServerPrx server) {
		this.songPath = songPath;
		this.artist = artist;
		this.title = title;
		this.dialog = dialog;
		this.server = server;
	}

	@Override
	protected Void doInBackground(Void... params) {
		try {
			int offset = 0;
			File file = new File(songPath);
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			int max = 512 * 1024;
			size = (int) file.length();
			byte[] bytes = new byte[size];
			int i = in.read(bytes);
			dialog.set(size, artist, title);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setProgressNumberFormat("%1d/%2dKB");
			while (offset < size) {
				int end = offset + max;
				if (end > size)
					end = size;
				byte[] temp = Arrays.copyOfRange(bytes, offset, end);
				try {
					publishProgress(offset);
					server.uploadFile(artist + "." + title, temp);
				} catch (Exception e) {
					Log.e("upload", e.toString());
				}
				offset += end;
			}
			dialog.dismiss();
			server.addSong(new Song(artist, title, artist + "." + title + ".mp3"));
		} catch (Exception e) {
			Log.e("upload", e.toString());
		}
		return null;
	}

	@Override
	public void onProgressUpdate(Integer... values)
	{
		if (values[0] == 0) {
			dialog.setMessage("Uploading " + title + " by " + artist);
			dialog.setMax(size);
			dialog.show();
		}
		dialog.setProgress(values[0]);
	}

	@Override
	public void onPostExecute(Void result)
	{
		if (this.dialog != null)
			this.dialog.dismiss();
	}
}
