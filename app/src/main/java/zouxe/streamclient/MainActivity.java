package zouxe.streamclient;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

        sp.Search(artist, title);

        titleText.setText("");
        artistText.setText("");
    }

    public void add(View addView) {
        EditText titleText = (EditText)findViewById(R.id.titleAddText);
        EditText artistText = (EditText)findViewById(R.id.artistAddText);
        String title = titleText.getText().toString();
        String artist = artistText.getText().toString();
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
