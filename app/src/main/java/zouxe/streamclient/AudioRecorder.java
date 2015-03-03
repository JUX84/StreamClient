package zouxe.streamclient;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

class AudioRecorder {
    private AudioRecord recorder = null;
    private Boolean isRecording = false;
    private Thread recordingThread = null;
    private float[] audioData = null;
    private int index = 0;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BufferElements2Rec = 1024;

    public void record() {
        int bytesPerElement = 2;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * bytesPerElement);

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
        index = 0;
    }

    private void saveAudioData() {
        while (isRecording) {
            if(audioData == null)
                audioData = new float[2048];
            if(index<1024) {
                byte[] data = new byte[BufferElements2Rec];
                recorder.read(data, 0, BufferElements2Rec);
                for(int i = 0; i < data.length; i+=4)
                    audioData[index++] = ByteBuffer.wrap(data,i,4).getFloat();
            }
        }
    }
}
