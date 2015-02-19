package zouxe.streamclient;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Player.*;

/**
 * Created by JUX on 17/02/2015.
 */
public class StreamPlayer implements MediaPlayer.OnPreparedListener {
    Ice.Communicator ic = null;
    MediaPlayer mp = null;
    Player.ServerPrx server = null;
    String token = null;
    Song[] songs = null;
    Song selectedSong = null;

    public StreamPlayer(Activity activity) {
        try {
            TextView status = (TextView)activity.findViewById(R.id.statusVar);
            status.setText("Connecting");
            status.setTextColor(Color.YELLOW);
            ic = Ice.Util.initialize();
            Ice.ObjectPrx base = ic.stringToProxy("StreamServer:tcp -h zouxe.ovh -p 10000");
            server = Player.ServerPrxHelper.checkedCast(base);
            if (server == null) {
                status.setText("Couldn't connect");
                status.setTextColor(Color.RED);
                return;
            }
            status.setText("Connected");
            status.setTextColor(Color.GREEN);
            mp = new MediaPlayer();
        } catch (Ice.LocalException e) {
        }
    }

    private void selectSong(Song s) {
        selectedSong = s;
        if(server == null)
            return;
    }

    public void removeSong() {
        if(selectedSong == null || server == null)
            return;
        server.removeSong(selectedSong);
        selectedSong = null;
    }

    public Boolean Start() {
        if(selectedSong == null || server == null)
            return false;
        if(token!=null)
            server.stopSong(token);
        token = server.selectSong(selectedSong);
        server.playSong(token);
        String mp3 = "http://zouxe.ovh:8090/"+token+".mp3";
        mp.reset();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(this);
        try {
            mp.setDataSource(mp3);
        } catch (IOException e) {
            System.out.println(e);
        }
        mp.prepareAsync();
        return true;
    }

    public void Pause() {
        if(server == null || token == null)
            return;
        mp.pause();
    }

    public void Play() {
        if(server == null || token == null)
            return;
        mp.start();
    }

    public void Search(String artist, String title, Activity activity) {
        if(server == null)
            return;

        songs = server.searchSong(artist, title);

        final String c1 = "title";
        final String c2 = "artist";

        List data = new ArrayList<HashMap<String, String>>();

        for (Song s : songs) {
            HashMap<String, String> e = new HashMap<String, String>();

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
                selectSong(songs[position]);
            }
        });
    }

    public void addSong(String artist, String title) {
        if(server == null)
            return;
        server.addSong(new Song(artist, title, artist+"."+title+".mp3"));
    }

    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}
