package zouxe.streamclient;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends ActionBarActivity {
    private StreamPlayer sp = null;
    private AudioRecorder recorder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(sp == null)
            new StreamPlayerLoader().run(this);
        if(recorder == null)
            recorder = new AudioRecorder();
    }

    public void search(View searchView) {

        EditText artistText = (EditText)findViewById(R.id.artistSearchText);
        EditText titleText = (EditText)findViewById(R.id.titleSearchText);

        String artist = artistText.getText().toString();
        String title = titleText.getText().toString();

        sp.Search(artist, title);

        titleText.setText("");
        artistText.setText("");
    }

    public void add(View addView) {
        EditText titleText = (EditText)findViewById(R.id.titleAddText);
        EditText artistText = (EditText)findViewById(R.id.artistAddText);
        String title = titleText.getText().toString();
        String artist = artistText.getText().toString();
        if(title.isEmpty())
            titleText.setHintTextColor(Color.RED);
        else
            titleText.setHintTextColor(Color.LTGRAY);
        if(artist.isEmpty())
            artistText.setHintTextColor(Color.RED);
        else
            artistText.setHintTextColor(Color.LTGRAY);
        if(title.isEmpty() || artist.isEmpty())
            return;
        sp.addSong(artist, title);
        titleText.setText("");
        artistText.setText("");
        sp.Search("", "");
    }

    public void play(View controlView) {
        Button controlButton = (Button)findViewById(R.id.controlButton);
        if(controlButton.getText().equals(getString(R.string.pause)))
            sp.Pause();
        else if(controlButton.getText().equals(getString(R.string.start)))
            sp.Start();
        else if(controlButton.getText().equals(getString(R.string.play)))
            sp.Play();
    }

    public void record(View recordView) {
        Button recordButton = (Button)findViewById(R.id.recordButton);
        if(recordButton.getText().equals(getString(R.string.record))) {
            if(null != recorder) {
                recorder.record();
                recordButton.setText(R.string.stop);
            }
        } else if(recordButton.getText().equals(getString(R.string.stop))){
            if (null != recorder) {
                recorder.stopRecord();
                recordButton.setText(getString(R.string.record));
            }
        }
    }

    public void remove(View removeView) {
        sp.removeSong();
        sp.Search("", "");
    }

    private class StreamPlayerLoader extends Thread {
        public void run(Activity act) {
            sp = new StreamPlayer(act);
            sp.Search("", "");
        }
    }
}
