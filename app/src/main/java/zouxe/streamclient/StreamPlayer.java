package zouxe.streamclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Player.*;

class StreamPlayer implements MediaPlayer.OnPreparedListener {
    private MediaPlayer mp = null;
    private Player.ServerPrx server = null;
    private String token = null;
    private Song[] songs = null;
    private Song playingSong = null;
    private Song selectedSong = null;
    private View v = null;
    private Button controlButton = null;
    private Button removeButton = null;
    private boolean isLoading = false;
    private Activity activity = null;

    public StreamPlayer(Activity activity) {
        this.activity = activity;

        controlButton = (Button)activity.findViewById(R.id.controlButton);
        removeButton = (Button)activity.findViewById(R.id.removeButton);
        try {
            setStatus(activity.getString(R.string.connecting));
            Ice.Communicator ic = Ice.Util.initialize();
            Ice.ObjectPrx base = ic.stringToProxy("StreamServer:tcp -h zouxe.ovh -p 10000");
            server = Player.ServerPrxHelper.checkedCast(base);
            if(server == null) {
                new AlertDialog.Builder(activity).setMessage(R.string.disconnectedReasonCast).create().show();
                setStatus(activity.getString(R.string.disconnected));
            }
            setStatus(activity.getString(R.string.connected));
            mp = new MediaPlayer();
            Button b = (Button)activity.findViewById(R.id.addButton);
            b.setEnabled(true);
            b = (Button)activity.findViewById(R.id.searchButton);
            b.setEnabled(true);
        } catch (Ice.LocalException e) {
            new AlertDialog.Builder(activity).setMessage(R.string.disconnectedReasonServer).create().show();
            setStatus(activity.getString(R.string.disconnected));
            System.err.println(e.getMessage());
        }
    }

    private void setStatus(String str) {
        TextView status = (TextView)activity.findViewById(R.id.serverStatus);
        status.setText(activity.getString(R.string.serverStatus)+" "+str);
    }

    private void selectSong(Song s) {
        selectedSong = s;
        if(playingSong != null && playingSong.equals(selectedSong)) {
            if (isLoading) {
                controlButton.setText(activity.getString(R.string.loading));
                controlButton.setEnabled(false);
            } else {
                if(mp.isPlaying())
                    controlButton.setText(activity.getString(R.string.pause));
                else
                    controlButton.setText(activity.getString(R.string.play));
                controlButton.setEnabled(true);
            }
        } else {
            controlButton.setText(activity.getString(R.string.start));
            controlButton.setEnabled(true);
        }
        removeButton.setEnabled(true);
    }

    public void removeSong() {
        if(selectedSong == null || server == null)
            return;
        if(playingSong==selectedSong)
            Pause();
        server.removeSong(selectedSong);
        selectedSong = null;
        controlButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

    public void Start() {
        if(selectedSong == null || server == null)
            return;
        if(token!=null) {
            mp.stop();
            server.stopSong(token);
        }
        playingSong = selectedSong;
        controlButton.setText(activity.getString(R.string.loading));
        controlButton.setEnabled(false);
        token = server.selectSong(selectedSong);
        server.playSong(token);
        String mp3 = "http://zouxe.ovh:8090/"+token+".mp3";
        mp.reset();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(this);
        try {
            mp.setDataSource(mp3);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        isLoading = true;
        mp.prepareAsync();
    }

    public void Pause() {
        if(server == null || token == null)
            return;
        controlButton.setText(activity.getString(R.string.play));
        controlButton.setEnabled(true);
        mp.pause();
    }

    public void Play() {
        if(server == null || token == null)
            return;
        if(selectedSong.equals(playingSong)) {
            controlButton.setText(activity.getString(R.string.pause));
            controlButton.setEnabled(true);
        }
        isLoading = false;
        mp.start();
    }

    public void Search(String artist, String title) {
        if(server == null)
            return;
        new SearchSongLoader().run(artist,title);
    }

    public void addSong(String artist, String title) {
        if(server == null)
            return;
        server.addSong(new Song(artist, title, artist+"."+title+".mp3"));
    }

    public void onPrepared(MediaPlayer mp) {
        Play();
    }

    private class SearchSongLoader extends Thread {
        public void run(String artist, String title) {
            songs = server.searchSong(artist, title);

            final String c1 = "title";
            final String c2 = "artist";

            List<HashMap<String, String>> data = new ArrayList<>();

            for (Song s : songs) {
                HashMap<String, String> e = new HashMap<>();

                e.put(c1, s.title);
                e.put(c2, s.artist);
                data.add(e);
            }

            SimpleAdapter adapter = new SimpleAdapter(activity,
                    data,
                    android.R.layout.simple_list_item_2,
                    new String[]{c1, c2},
                    new int[]{android.R.id.text1,
                            android.R.id.text2});
            ListView lv = (ListView)activity.findViewById(R.id.listView);
            lv.setAdapter(adapter);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    if(v!=null)
                        v.setBackgroundColor(Color.TRANSPARENT);
                    view.setBackgroundColor(Color.LTGRAY);
                    v = view;
                    selectSong(songs[position]);
                }
            });
        }
    }
}
