package zouxe.streamclient;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AudioRecorder {
    private AudioRecord recorder = null;
    private Boolean isRecording = false;
    private Thread recordingThread = null;
    private static final int REC_SR = 8000;
    private static final int REC_CHAN = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_CHAN = AudioFormat.CHANNEL_OUT_MONO;
    private static final int REC_ENC = AudioFormat.ENCODING_PCM_16BIT;
    private static final int bufferSize = 1024;

    public void record() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                REC_SR, REC_CHAN,
                REC_ENC, bufferSize);
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
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
        recordingThread = null;
    }

    private void saveAudioData() {
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, REC_SR, AUDIO_CHAN, REC_ENC, bufferSize, AudioTrack.MODE_STREAM);
        float[] audioData = new float[bufferSize/4];
        track.play();
        while (isRecording) {
            byte[] data = new byte[bufferSize];
            recorder.read(data, 0, bufferSize);
            ByteBuffer tmp;
            for(int i = 0; i < bufferSize; i+=4) {
                tmp = ByteBuffer.wrap(data,i,4);
                audioData[i/4] = tmp.order(ByteOrder.LITTLE_ENDIAN).getFloat();
            }
            byte[] audio = new byte[bufferSize];
            int j = 0;
            for(int i = 0; i < bufferSize/4; i++) {
                tmp = ByteBuffer.allocate(4);
                byte[] arr = tmp.order(ByteOrder.LITTLE_ENDIAN).putFloat(audioData[i]).array();
                for(byte b : arr)
                    audio[j++] = b;
            }
            track.write(audio, 0, bufferSize);
        }
    }
}
