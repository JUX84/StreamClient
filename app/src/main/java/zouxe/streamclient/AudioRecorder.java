package zouxe.streamclient;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import speeral.SpeeralError;

class AudioRecorder {
	private static final int REC_SR = 8000;
	private static final int REC_CHAN = AudioFormat.CHANNEL_IN_MONO;
	private static final int REC_ENC = AudioFormat.ENCODING_PCM_16BIT;
	private static final int bufferSize = 1024;
	private AudioRecord recorder = null;
	private Boolean isRecording = false;
	private Ice.Communicator communicator = null;
	private speeral.ServerPrx server = null;
	private short[][] audioData;
	private int current;

	public void record() {
		if (communicator == null)
			initIce();
		initServer();
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				REC_SR, REC_CHAN,
				REC_ENC, bufferSize);
		recorder.startRecording();
		isRecording = true;
		Thread recordingThread = new Thread(new Runnable() {
			public void run() {
				saveAudioData();
			}
		});
		recordingThread.start();
		audioData = new short[2048][];
		current = 0;
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
			Ice.ObjectPrx base = communicator.stringToProxy("SpeeralServer:tcp -h 188.226.241.233 -p 10000");
			server = speeral.ServerPrxHelper.checkedCast(base);
		} catch (Exception e) {
			Log.e("Speeral", e.toString());
		}
	}

	public void stopRecord() {
		isRecording = false;
		recorder.stop();
		recorder.release();
		recorder = null;
		int i = 0;
		for (int j = 0; j < current; ++j) {
			i += audioData[j].length;
		}
		short[] data = new short[i];
		i = 0;
		for (int j = 0; j < current; ++j) {
			for (short s : audioData[j]) {
				data[i++] = s;
			}
		}
		try {
			Log.v("Output", server.decode(data, true));
		} catch (SpeeralError e) {
			Log.e("Speeral", e.toString());
		}
	}

	private void saveAudioData() {
		while (isRecording) {
			audioData[current] = new short[bufferSize];
			recorder.read(audioData[current++], 0, bufferSize);
		}
	}
}
