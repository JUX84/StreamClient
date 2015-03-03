package zouxe.streamclient;

import android.app.Activity;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.nio.ByteBuffer;


public class MainActivity extends ActionBarActivity {
    private StreamPlayer sp = null;
    private AudioRecord recorder = null;
    private Boolean isRecording = false;
    private Thread recordingThread = null;
    private float[] audioData = null;
    private int index = 0;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("CREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(sp == null)
            new StreamPlayerLoader().run(this);
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        super.onConfigurationChanged(conf);
        if(sp != null) {
            Button b = (Button) findViewById(R.id.controlButton);
            if(sp.isPlaying())
                b.setText("Pause");
            else if (sp.isSongSelected())
                b.setText("Play");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        //return true;
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        //return super.onOptionsItemSelected(item);
        return false;
    }

    public void search(View searchView) {

        EditText artistText = (EditText)findViewById(R.id.artistSearchText);
        EditText titleText = (EditText)findViewById(R.id.titleSearchText);

        String artist = artistText.getText().toString();
        String title = titleText.getText().toString();

        sp.Search(artist, title, this);

        titleText.setText("");
        artistText.setText("");
    }

    public void add(View addView) {
        EditText titleText = (EditText)findViewById(R.id.titleAddText);
        EditText artistText = (EditText)findViewById(R.id.artistAddText);
        String title = titleText.getText().toString();
        String artist = artistText.getText().toString();
        if(title.isEmpty() || artist.isEmpty() || title.equals("title") || artist.equals("artist"))
            return;
        sp.addSong(artist, title);
        titleText.setText("");
        artistText.setText("");
        sp.Search("", "", this);
    }

    public void play(View controlView) {
        Button controlButton = (Button)findViewById(R.id.controlButton);
        if(controlButton.getText().equals("Start")) {
            if (sp.Start())
                controlButton.setText("Pause");
        } else if(controlButton.getText().equals("Pause")){
            sp.Pause();
            controlButton.setText("Play");
        } else if(controlButton.getText().equals("Play")){
            sp.Play();
            controlButton.setText("Pause");
        }
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    public void record(View recordView) {
        Button recordButton = (Button)findViewById(R.id.recordButton);
        if(recordButton.getText().equals("Record")) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

            recorder.startRecording();
            isRecording = true;
            recordingThread = new Thread(new Runnable() {
                public void run() {
                    writeAudioDataToFloat();
                }
            });
            recordingThread.start();
            recordButton.setText("Stop");
        } else if(recordButton.getText().equals("Stop")){
            if (null != recorder) {
                isRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
                index = 0;
            }
            recordButton.setText("Record");
        }
    }

    public void writeAudioDataToFloat() {
        while (isRecording) {
            if(audioData == null)
                audioData = new float[256];
            if(index<1024) {
                byte[] data = new byte[BufferElements2Rec];
                recorder.read(data, 0, BufferElements2Rec);
                audioData[index++] = ByteBuffer.wrap(data).getFloat();
            }
        }
    }

    public void remove(View removeView) {
        sp.removeSong();
        sp.Search("", "", this);
    }

    private class StreamPlayerLoader extends Thread {
        public void run(Activity act) {
            sp = new StreamPlayer(act);
            sp.Search("", "", act);
        }
    }
}
