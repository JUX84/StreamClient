package zouxe.streamclient;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

import Player.*;

/**
 * Created by JUX on 17/02/2015.
 */
public class StreamPlayer implements MediaPlayer.OnPreparedListener {
    Ice.Communicator ic = null;
    MediaPlayer mp = null;
    Player.ServerPrx server = null;

    public StreamPlayer() {
        try {
            ic = Ice.Util.initialize();
            Ice.ObjectPrx base = ic.stringToProxy("StreamServer:tcp -h zouxe.ovh -p 10000");
            server = Player.ServerPrxHelper.checkedCast(base);
            if (server == null) {
                return;
            }
            mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setOnPreparedListener(this);
        } catch (Ice.LocalException e) {
            System.out.println(e);
        }
    }

    public void Play() {
        if(server == null)
            return;
        String token = server.selectSong(new Song());
        server.playSong(token);
        String mp3 = "http://zouxe.ovh:8090/"+token+".mp3";
        try {
            mp.setDataSource(mp3);
        } catch (IOException e) {
            System.out.println(e);
        }
        mp.prepareAsync();
    }

    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}
