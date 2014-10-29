package dk.dr.radio.afspilning.wrapper;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;

import java.io.IOException;

import dk.dr.exoplayer.DefaultRendererBuilder;
import dk.dr.exoplayer.DemoPlayer;
import dk.dr.exoplayer.EventLogger;
import dk.dr.exoplayer.HlsRendererBuilder;
import dk.dr.radio.diverse.App;

/**
 * Wrapper til ExoPlayer.
 * @author Jacob Nordfalk 28-11-14.
 */
public class ExoPlayerWrapper implements MediaPlayerWrapper, DemoPlayer.Listener {

  DemoPlayer player;

  private EventLogger eventLogger;
  private MediaPlayerLytter lytter;

  @Override
  public void setDataSource(final String url) throws IOException {
    App.forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        String versionName;
        versionName = "ExoPlayerDemo/" + App.versionsnavn + " (Linux;Android " + Build.VERSION.RELEASE + ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
        if (url.endsWith("m3u8")) {
          player = new DemoPlayer(new HlsRendererBuilder(versionName, url, url));
          App.kortToast("HlsRendererBuilder\n" + url);
        } else {
          player = new DemoPlayer(new DefaultRendererBuilder(App.instans, Uri.parse(url), null));
          App.kortToast("DefaultRendererBuilder\n" + url);
        }
        player.addListener(ExoPlayerWrapper.this);
        player.seekTo(0);
        eventLogger = new EventLogger();
        eventLogger.startSession();
        player.addListener(eventLogger);
        player.setInfoListener(eventLogger);
        player.setInternalErrorListener(eventLogger);
      }
    });
  }

  @Override
  public void setAudioStreamType(int streamMusic) {
    //TODO player.setAudioStreamType(streamMusic);
  }

  @Override
  public void prepare() throws IOException {
    App.forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        player.prepare();
        player.setPlayWhenReady(true);
      }
    });
  }

  @Override
  public void seekTo(int offsetMs) {
    player.seekTo(offsetMs);
  }

  @Override
  public int getDuration() {
    return (int) player.getDuration();
  }

  @Override
  public int getCurrentPosition() {
    return (int) player.getCurrentPosition();
  }

  @Override
  public void start() {
    player.getPlayerControl().start();
  }


  @Override
  public void stop() {
    player.getPlayerControl().pause();
  }

  @Override
  public void release() {
    player.release();
  }

  @Override
  public void reset() {
    //player.reset();
  }

  @Override
  public boolean isPlaying() {
    return player.getPlaybackState()== ExoPlayer.STATE_READY;
  }

  @Override
  public void setWakeMode(Context ctx, int screenDimWakeLock) {
    //player.setWakeMode(ctx, screenDimWakeLock);
  }

  @Override
  public void setMediaPlayerLytter(MediaPlayerLytter lytter) {
    this.lytter = lytter;
  }

  @Override
  public void onStateChanged(boolean playWhenReady, int playbackState) {
    if (lytter==null) return;
    switch (playbackState) {
      case ExoPlayer.STATE_BUFFERING:
        lytter.onBufferingUpdate(null, player.getBufferedPercentage());
        break;
      case ExoPlayer.STATE_ENDED:
        lytter.onCompletion(null);
        break;
      case ExoPlayer.STATE_IDLE:
        lytter.onCompletion(null);
        //text += "idle";
        break;
      case ExoPlayer.STATE_PREPARING:
        lytter.onBufferingUpdate(null, 100);
        break;
      case ExoPlayer.STATE_READY:
        lytter.onPrepared(null);
        break;
      default:
        break;
    }
  }

  @Override
  public void onError(Exception e) {
    lytter.onError(null, 42, 42);
  }

  @Override
  public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {

  }
}
