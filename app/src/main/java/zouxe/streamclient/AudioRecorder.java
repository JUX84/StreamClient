package zouxe.streamclient;

/**
 * Created by JUX on 30/04/2015.
 */
interface AudioRecorder {
	void record();
	void stopRecord();
	boolean isRecording();
}
