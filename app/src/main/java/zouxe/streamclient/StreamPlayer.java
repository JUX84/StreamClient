package zouxe.streamclient;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.widget.TextView;

import java.io.IOException;

import Player.*;

/**
 * Created by JUX on 17/02/2015.
 */
public class StreamPlayer implements MediaPlayer.OnPreparedListener {
    Ice.Communicator ic = null;
    MediaPlayer mp = null;
    Player.ServerPrx server = null;
    String token = null;

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
            server.addSong(new Song("test", "test", "test.mp3"));
            mp = new MediaPlayer();
        } catch (Ice.LocalException e) {
        }
    }

    public void Play() {
        if(server == null)
            return;
        token = server.selectSong(new Song("test","test","test.mp3"));
        server.playSong(token);
        String mp3 = "http://zouxe.ovh:8090/"+token+".mp3";
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(this);
        try {
            mp.setDataSource(mp3);
        } catch (IOException e) {
            System.out.println(e);
        }
        mp.prepareAsync();
    }

    public void Stop() {
        if(server == null || token == null)
            return;
        mp.reset();
        server.stopSong(token);
    }

    public void Search(String artist, String title) {
        Song[] songs = server.searchSong(artist, title);
    }

    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}
