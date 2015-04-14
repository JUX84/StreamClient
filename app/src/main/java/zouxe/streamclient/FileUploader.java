package zouxe.streamclient;

import Player.Song;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * Created by JUX on 14/04/2015.
 */
class FileUploader {
	private String songPath = null;
	private String artist = null;
	private String title = null;
	private CustomProgressDialog dialog = null;
	private Player.ServerPrx server = null;

	public FileUploader(String songPath, String artist, String title, CustomProgressDialog dialog, Player.ServerPrx server) {
		this.songPath = songPath;
		this.artist = artist;
		this.title = title;
		this.dialog = dialog;
		this.server = server;
	}

	public void upload() {
		try {
			int offset = 0;
			File file = new File(songPath);
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			int max = 512 * 1024;
			final int size = (int) file.length();
			byte[] bytes = new byte[size];
			in.read(bytes);
			dialog.set(size, artist, title);
			while (offset < size) {
				int end = offset + max;
				if (end > size)
					end = size;
				final int tmp = end;
				byte[] temp = Arrays.copyOfRange(bytes, offset, end);
				try {
					server.uploadFile(artist + "." + title, temp);
					dialog.setProgress(tmp);
				} catch (Exception e) {
					Log.e("upload", e.toString());
				}
			}
			dialog.dismiss();
			server.addSong(new Song(artist, title, artist + "." + title));
		} catch (Exception e) {
			Log.e("upload", e.toString());
		}
	}
}
