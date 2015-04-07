package zouxe.streamclient;

import PocketSphinxIce.*;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;

import java.util.Arrays;

class AudioRecorder {
	private static final int REC_SR = 16000;
	private static final int REC_CHAN = AudioFormat.CHANNEL_IN_MONO;
	private static final int REC_ENC = AudioFormat.ENCODING_PCM_16BIT;
	private static int bufferSize;
	private AudioRecord recorder = null;
	private Ice.Communicator communicator = null;
	private IPocketSphinxServerPrx server = null;
	private short[] audioData;
	private int current;
	private final Activity activity;
	private final Button recordButton;

	public AudioRecorder(Ice.Communicator communicator, Activity activity) {
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
		audioData = new short[bufferSize*100];
		current = 0;
		new Thread(new Runnable() {
			public void run() {
				int tmp;
				while (current < bufferSize*100 && recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
					tmp = recorder.read(audioData, current, bufferSize);
					current += tmp;
				}
				stopRecord();
			}
		}).start();
	}

	private void initServer() {
		try {
			Ice.ObjectPrx base = communicator.stringToProxy("PocketSphinxServer:tcp -h 188.226.241.233 -p 20000");
			server = PocketSphinxIce.IPocketSphinxServerPrxHelper.checkedCast(base);
		} catch (Exception e) {
			Log.e("PocketSphinxServer", e.toString());
		}
	}

	public void stopRecord() {
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
						Log.v("Output", server.decode(Arrays.copyOf(audioData, current)));
					} catch (PocketSphinxIce.Error e) {
						Log.e("PocketSphinx", e.toString());
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
