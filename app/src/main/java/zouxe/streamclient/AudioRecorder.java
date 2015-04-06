package zouxe.streamclient;

import PocketSphinxIce.*;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

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

	public AudioRecorder() {
		if (communicator == null)
			initIce();
		initServer();
	}

	public void record() {
		bufferSize = AudioRecord.getMinBufferSize(REC_SR, REC_CHAN, REC_ENC);
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				REC_SR, REC_CHAN,
				REC_ENC, bufferSize);
		recorder.startRecording();
		audioData = new short[bufferSize*100];
		current = 0;
		new Thread(new Runnable() {
			public void run() {
			while (current < bufferSize*100 && recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				int tmp = recorder.read(audioData, current, bufferSize);
				current += tmp;
			}
			}
		}).start();
	}

	private void initIce() {
		try {
			communicator = Ice.Util.initialize();
		} catch (Exception e) {
			Log.e("Ice", e.toString());
		}
	}

	private void initServer() {
		if (communicator == null)
			return;
		try {
			Ice.ObjectPrx base = communicator.stringToProxy("PocketSphinxServer:tcp -h 188.226.241.233 -p 20000");
			server = PocketSphinxIce.IPocketSphinxServerPrxHelper.checkedCast(base);
		} catch (Exception e) {
			Log.e("PocketSphinxServer", e.toString());
		}
	}

	public void stopRecord() {
		recorder.stop();
		new Thread(new Runnable() {
			public void run() {
				try {
					Log.v("Output", server.decode(Arrays.copyOf(audioData, current)));
				} catch (PocketSphinxIce.Error e) {
					Log.e("PocketSphinx", e.toString());
				}
			}
		}).start();
	}
}
