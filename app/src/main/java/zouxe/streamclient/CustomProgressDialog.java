package zouxe.streamclient;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by JUX on 14/04/2015.
 */
public class CustomProgressDialog extends ProgressDialog {
	public CustomProgressDialog(Context context) {
		super(context);
	}

	public void set(int size, String artist, String title) {
		setMessage("Uploading " + title + " by " + artist);
		setMax(size);
	}
}
