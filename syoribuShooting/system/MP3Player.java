package syoribuShooting.system;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

// new すれば引数のURLのサウンドを再生してくれるクラス
public class MP3Player
{
    private static boolean isEnable = true;

    private URL soundUrl;
    private boolean isLoop; //ループするならtrue
    private LoopPlaybackListener loopListener; //再生し終わった時のイベントをlistenする用
    private PlayThread pthread;

    public MP3Player(URL surl, boolean loop)
    {
        if (isEnable()) {
            this.soundUrl = surl;
            this.isLoop = loop;
            this.loopListener = new LoopPlaybackListener();
            //新しいスレッドで再生
            this.pthread = new PlayThread();
            this.pthread.start();
        }
    }
    
    
    //ループ再生中止
    public void stop()
    {
        if (!isEnable()) return;

        this.isLoop = false;
        if (pthread != null &&  pthread.player != null) {
            this.pthread.player.stop();
        }
    }

    public static void setEnable(boolean enable)
    {
        isEnable = enable;
    }

    public static boolean isEnable()
    {
        return isEnable;
    }

    //ループ用リスナー
    class LoopPlaybackListener extends PlaybackListener
    {
        @Override
        public void playbackFinished(PlaybackEvent ev)
        {
            if (isLoop) {
                pthread = new PlayThread();
                pthread.start();
            }
        }
    }
    // 再生スレッドクラス
    class PlayThread extends Thread
    {
        private AdvancedPlayer player = null;
        @Override
        public void run()
        {
            InputStream istream = null;
            try {
                //再生準備
                istream = soundUrl.openStream();
                player = new AdvancedPlayer(istream);
                player.setPlayBackListener(MP3Player.this.loopListener);
                //再生
                player.play();
            } catch (JavaLayerException | IOException e) {
                e.printStackTrace();
            } finally {
                //再生が終わったら閉じる
                if (player != null) player.close();
                try {
                    if (istream != null)istream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
