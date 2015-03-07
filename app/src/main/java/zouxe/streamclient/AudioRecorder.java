package zouxe.streamclient;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class AudioRecorder {
	private static final int REC_SR = 8000;
	private static final int REC_CHAN = AudioFormat.CHANNEL_IN_MONO;
	private static final int REC_ENC = AudioFormat.ENCODING_PCM_16BIT;
	private static final int bufferSize = 1024;
	private AudioRecord recorder = null;
	private Boolean isRecording = false;
	private List<Float> audioData = null;

	public void record() {
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
	}

	public void stopRecord() {
		isRecording = false;
		recorder.stop();
		recorder.release();
		recorder = null;
	}

	private void saveAudioData() {
		if (audioData == null)
			audioData = new ArrayList<>();
		else
			audioData.clear();
		while (isRecording) {
			byte[] data = new byte[bufferSize];
			recorder.read(data, 0, bufferSize);
			ByteBuffer tmp;
			for (int i = 0; i < bufferSize; i += 4) {
				tmp = ByteBuffer.wrap(data, i, 4);
				audioData.add(tmp.order(ByteOrder.LITTLE_ENDIAN).getFloat());
			}
		}
	}
}
